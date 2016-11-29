package org.aktin.report.archive;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.aktin.report.ArchivedReport;
import org.aktin.report.GeneratedReport;

public class ReportImpl implements ArchivedReport{
	private ReportArchiveImpl archive;
	private int id;
	private Path location;
	private String templateId;
	private String templateVersion;
	private Instant dataStart;
	private Instant dataEnd;
	private Instant dataTimestamp;
	private String mediaType;
	private String userId;
	/** report preferences. loaded on demand from the database */
	private Map<String,String> prefs;

	static final String selectReports = "SELECT id, template_id, template_version, data_start, data_end, data_timestamp, media_type, user_name, path FROM generated_reports";
	static final String selectNextReportId = "SELECT NEXTVAL('generated_reports_id')";
	static final String insertReport = "INSERT INTO generated_reports(id, template_id, template_version, data_start, data_end, data_timestamp, media_type, user_name, path) VALUES(?,?,?,?,?,?,?,?,?)";
	static final String deleteReport = "DELETE FROM generated_reports WHERE id=?";
	private static final String SELECT_REPORT_PREFS = "SELECT preferences FROM generated_reports WHERE id=?";
	
	private ReportImpl(ReportArchiveImpl archive){
		this.archive = archive;
	}

	static ReportImpl fromResultSet(ReportArchiveImpl archive, ResultSet rs) throws SQLException{
		ReportImpl r = new ReportImpl(archive);
		r.id = rs.getInt(1);
		r.templateId = rs.getString(2);
		r.templateVersion = rs.getString(3);
		r.dataStart = rs.getTimestamp(4).toInstant();
		r.dataEnd = rs.getTimestamp(5).toInstant();
		r.dataTimestamp = rs.getTimestamp(6).toInstant();
		r.mediaType = rs.getString(7);
		r.userId = rs.getString(8);
		r.location = archive.getDataDir().resolve(rs.getString(9));
		return r;
	}
	static int nextResultId(Connection dbc) throws SQLException{
		int id;
		Statement stmt = dbc.createStatement();
		ResultSet rs = stmt.executeQuery(ReportImpl.selectNextReportId);
		rs.next();
		id = rs.getInt(1);
		rs.close();
		stmt.close();
		return id;
	}

	static ReportImpl insertReport(Connection dbc, GeneratedReport report, String userId, ReportArchiveImpl archive) throws SQLException, IOException{
		ReportImpl ri = new ReportImpl(archive);
		ri.id = nextResultId(dbc);
		ri.templateId = report.getTemplateId();
		ri.templateVersion = report.getTemplateVersion();
		ri.dataStart = report.getStartTimestamp();
		ri.dataEnd = report.getEndTimestamp();
		ri.dataTimestamp = report.getDataTimestamp();
		ri.mediaType = report.getMediaType();
		ri.userId = userId;
		ri.moveGeneratedReportFiles(report.getLocation());
		ri.insertIntoTable(dbc);
		return ri;
	}
	private void insertIntoTable(Connection dbc) throws SQLException{
		try( PreparedStatement s = dbc.prepareStatement(insertReport) ){
			s.setInt(1, getId());
			s.setString(2, getTemplateId());
			s.setString(3, getTemplateVersion());
			s.setTimestamp(4, Timestamp.from(getStartTimestamp()));
			s.setTimestamp(5, Timestamp.from(getEndTimestamp()));
			s.setTimestamp(6, Timestamp.from(getDataTimestamp()));
			s.setString(7, getMediaType());
			s.setString(8, getUserId());
			s.setString(9, archive.getDataDir().relativize(getLocation()).toString());
			s.executeUpdate();
		}
	}
	/**
	 * Move the report from the specified location to a new location in this
	 * archive. This will set the {@link #location} to the destination calculated
	 * by using data extraction year and id number.
	 *
	 * @param oldLocation
	 * @throws IOException
	 */
	private void moveGeneratedReportFiles(Path oldLocation) throws IOException{
		// group reports by year of creation/extraction
		int group = getDataTimestamp().get(ChronoField.YEAR);
		String suffix;
		if( getMediaType().equals("application/pdf") && Files.isRegularFile(oldLocation) ){
			suffix = ".pdf";
		}else if( Files.isDirectory(oldLocation) ){
			suffix = ""; // no suffix for directory
		}else{
			throw new IOException("Unable to determine file suffix for report with type "+getMediaType());
		}
		Path dest = archive.getDataDir().resolve(group + "/" + getId()+suffix);
		Files.move(oldLocation, dest);
		this.location = dest;
	}

	@Override
	public String getMediaType() {
		return mediaType;
	}

	@Override
	public Path getLocation() {
		return location;
	}

	@Override
	public Instant getStartTimestamp() {
		return dataStart;
	}

	@Override
	public Instant getEndTimestamp() {
		return dataEnd;
	}

	@Override
	public Instant getDataTimestamp() {
		return dataTimestamp;
	}

	@Override
	public String getTemplateId() {
		return templateId;
	}

	@Override
	public String getTemplateVersion() {
		return templateVersion;
	}

	private void readPreferences(Reader reader) throws IOException{
		Properties props = new Properties();
		props.load(reader);
		this.prefs = new HashMap<>();
		for( String key : props.stringPropertyNames() ){
			prefs.put(key, props.getProperty(key));
		}		
	}
	@Override
	public Map<String, String> getPreferences() {
		// lazy load
		if( this.prefs == null ){
			// TODO get datasource from archive
			try( Connection dbc = null ){
				PreparedStatement ps = dbc.prepareStatement(SELECT_REPORT_PREFS);
				ps.setInt(1, getId());
				ResultSet rs = ps.executeQuery();
				rs.next();
				try( Reader reader = rs.getCharacterStream(1) ){
					readPreferences(reader);
				}
				rs.close();
				ps.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch( IOException e ){
				// TODO
				e.printStackTrace();
			}
		}
		return prefs;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getUserId() {
		return userId;
	}

}
