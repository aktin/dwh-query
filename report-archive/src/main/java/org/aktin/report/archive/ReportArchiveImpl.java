package org.aktin.report.archive;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.ArchivedReport;
import org.aktin.report.GeneratedReport;
import org.aktin.report.ReportArchive;
import org.aktin.report.ReportInfo;

@Singleton
public class ReportArchiveImpl implements ReportArchive{
//	private Preferences prefs;
	/** directory to store reports */
	private Path dataDir;
	/** directory to store deleted reports (offline storage) */
	private Path archiveDir;
	DataSource ds;
	private List<ReportImpl> reports;
	boolean useHsql;

	public ReportArchiveImpl(){
	}

	public ReportArchiveImpl(DataSource ds, Path dataDir, Path archiveDir){
		this.ds = ds;
		this.dataDir = dataDir;
		this.archiveDir = archiveDir;
		this.useHsql = true;
	}
	@Inject
	public void setPreferences(Preferences prefs){
//		this.prefs = prefs;
		this.dataDir = Paths.get(prefs.get(PreferenceKey.reportDataPath));
		this.archiveDir = Paths.get(prefs.get(PreferenceKey.reportArchivePath));
	}

	@Resource(lookup="java:jboss/datasources/AktinDS")
	public void setDataSource(DataSource ds){
		this.ds = ds;
	}

	Path getDataDir(){
		return dataDir;
	}
	@PostConstruct
	public void loadArchive(){
		reports = new ArrayList<>();
		try( Connection dbc = ds.getConnection() ){
			Statement st = dbc.createStatement();
			ResultSet rs = st.executeQuery(ReportImpl.selectReports);
			while( rs.next() ){
				reports.add(ReportImpl.fromResultSet(this, rs));
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			throw new java.lang.IllegalStateException("Failed to load generated report metadata from database", e);
		}
	}

	@Override
	public ReportImpl addReport(ReportInfo report, String userId) throws IOException {
		ReportImpl r;
		try( Connection dbc = ds.getConnection() ){
			r = ReportImpl.insertReport(dbc, report, userId, this);
		} catch (SQLException e) {
			throw new IOException("Unable to write report info to database", e);
		}
		// add to reports
		reports.add(r);
		return r;
	}

	private int getReportIndex(int id){
		for( int i=0; i<reports.size(); i++ ){
			ReportImpl report = reports.get(i);
			if( report.getId() == id ){
				return i;
			}
		}
		return -1;
	}
	@Override
	public ReportImpl get(int id) {
		int i = getReportIndex(id);
		if( i == -1 ){
			// not found
			return null;
		}else{
			return reports.get(i);
		}
	}

	@Override
	public int size() {
		return reports.size();
	}

	@Override
	public void deleteReport(int reportId) throws IOException{
		// find index
		int i = getReportIndex(reportId);
		if( i == -1){
			throw new FileNotFoundException("Report id not found: "+reportId);
		}

		// remove from cache list
		ReportImpl report = reports.remove(i);

		// resolve paths
		Path relativePath = dataDir.relativize(report.getLocation());
		if( relativePath.isAbsolute() ){
			throw new IOException("Unable to determine relative report path: "+report.getLocation());
		}
		Path targetPath = archiveDir.resolve(relativePath);
		Files.createDirectories(targetPath.getParent());

		// write metadata
		Path metaPath = archiveDir.resolve(relativePath.toString()+".properties");
		writeMetadataProperties(report, metaPath);

		// the report archive will be in an inconsistent state, if failure occurs below this point
		// e.g. the report file still exists in the data directory but is not anymore in the cache (fixed after restart)
		// or report file not anymore in the data folder, but still in the database (cannot be fixed, but should not occur)

		// move to archive path
		// TODO this may fail if moving directories between different file system partitions
		Files.move(report.getLocation(), targetPath);

		// delete from database
		deleteReportFromDatabase(reportId);
	}

	private void writeMetadataProperties(ReportImpl report, Path target) throws IOException {
		Properties props = new Properties();
		if(report.getStatus() == ArchivedReport.Status.Completed)
			props.putAll(report.getPreferences());
		props.setProperty("report.data.start", report.getStartTimestamp().toString());
		props.setProperty("report.data.end", report.getEndTimestamp().toString());
		props.setProperty("report.data.timestamp", report.getEndTimestamp().toString());
		props.setProperty("report.template.id", report.getTemplateId());
		props.setProperty("report.template.version", report.getTemplateVersion());
		props.setProperty("report.user", report.getUserId());
		props.setProperty("report.id", Integer.toString(report.getId()));
		props.setProperty("report.mediatype", report.getMediaType());
		try( OutputStream out = Files.newOutputStream(target)) {
			props.store(out, "Report configuration");
		}
	}

	private void deleteReportFromDatabase(int reportId) throws IOException{
		try( Connection dbc = ds.getConnection() ){
			dbc.setAutoCommit(true);
			PreparedStatement s = dbc.prepareStatement(ReportImpl.deleteReport);
			s.setInt(1, reportId);
			s.executeUpdate();
		} catch (SQLException e) {
			throw new IOException("Unable to delete report record from DB", e);
		}
	}
	@Override
	public Iterable<ReportImpl> reports() {
		return reports;
	}

	@Override
	public void setReportFailure(int id, String description, Throwable cause) throws IOException {
		ReportImpl report = get(id);
		Objects.requireNonNull(report);
		try( Connection dbc = ds.getConnection() ){
			dbc.setAutoCommit(true);
			report.setFailed(dbc, description, cause);
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	@Override
	public ReportImpl setReportResult(int id, GeneratedReport report) throws IOException {
		ReportImpl my = get(id);
		Objects.requireNonNull(my);
		try( Connection dbc = ds.getConnection() ){
			dbc.setAutoCommit(true);
			my.setData(dbc, report);
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return my;
	}

}
