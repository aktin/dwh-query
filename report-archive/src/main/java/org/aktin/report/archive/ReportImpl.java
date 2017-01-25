package org.aktin.report.archive;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aktin.report.ArchivedReport;
import org.aktin.report.GeneratedReport;
import org.aktin.report.InsufficientDataException;
import org.aktin.report.ReportInfo;
import org.aktin.report.ReportManager;

public class ReportImpl implements ArchivedReport{
	private ReportArchiveImpl archive;
	private int id;
	private Path location;
	private String templateId;
	private String templateVersion;
	private Instant dataStart;
	private Instant dataEnd;
	private Instant dataTimestamp;
	private Instant createdTimestamp;
	private String mediaType;
	private String userId;
	/** report preferences. loaded on demand from the database */
	private Map<String,String> prefs;

	private static final Logger log = Logger.getLogger(ReportImpl.class.getName());	
	private static final String MEDIATYPE_INSUFFICIENT_DATA = "text/vnd.error.insufficientdata";
	private static final String MEDIATYPE_FAILURE_STACKTRACE = "text/vnd.error.stacktrace";
	static final String selectReports = "SELECT id, template_id, template_version, data_start, data_end, created_timestamp, created_by, data_timestamp, media_type, path FROM generated_reports ORDER BY id";
	static final String selectNextReportId = "SELECT NEXTVAL('generated_reports_id')";
	static final String selectNextReportIdHsql = "CALL NEXT VALUE FOR generated_reports_id";
	static final String insertReport = "INSERT INTO generated_reports(id, template_id, template_version, data_start, data_end, created_timestamp, created_by, preferences) VALUES(?,?,?,?,?,?,?,?)";
	static final String updateReport = "UPDATE generated_reports SET media_type=?, path=?, data_timestamp=?, preferences=? WHERE id=?";
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
		r.createdTimestamp = rs.getTimestamp(6).toInstant();
		r.userId = rs.getString(7);
		Timestamp ts = rs.getTimestamp(8);
		if( ts != null ){
			r.dataTimestamp = ts.toInstant();			
		}
		r.mediaType = rs.getString(9);
		String path = rs.getString(10);
		if( path != null ){
			r.location = archive.getDataDir().resolve(path);			
		}
		return r;
	}
	static int nextResultId(Connection dbc) throws SQLException{
		int id;
		Statement stmt = dbc.createStatement();
		// TODO only use this during unit testing
		String query = ReportImpl.selectNextReportId;
		if( dbc.getClass().getName().startsWith("org.hsqldb") ){
			query = ReportImpl.selectNextReportIdHsql;
		}
		ResultSet rs = stmt.executeQuery(query);
		rs.next();
		id = rs.getInt(1);
		rs.close();
		stmt.close();
		return id;
	}

	static ReportImpl insertReport(Connection dbc, ReportInfo report, String userId, ReportArchiveImpl archive) throws SQLException, IOException{
		ReportImpl ri = new ReportImpl(archive);
		ri.id = nextResultId(dbc);
		ri.templateId = report.getTemplateId();
		ri.templateVersion = report.getTemplateVersion();
		ri.dataStart = report.getStartTimestamp();
		ri.dataEnd = report.getEndTimestamp();
//		ri.dataTimestamp = report.getDataTimestamp();
//		ri.mediaType = report.getMediaType();
		ri.createdTimestamp = Instant.now();
		ri.userId = userId;
		ri.prefs = new HashMap<>(report.getPreferences());
//		ri.moveGeneratedReportFiles(report.getLocation());
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
			s.setTimestamp(6, Timestamp.from(createdTimestamp));
			s.setString(7, getUserId());
			if( this.prefs != null && this.prefs.size() > 0 ){
				s.setString(8, getPreferencesClob());
			}else{
				s.setString(8, null);
			}
//			s.setString(7, getMediaType());
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
		String suffix;
		if( getMediaType().equals("application/pdf") && Files.isRegularFile(oldLocation) ){
			suffix = ".pdf";
		}else if( getMediaType().startsWith("text/") && Files.isRegularFile(oldLocation) ){
			suffix = ".txt";
		}else if( Files.isDirectory(oldLocation) ){
			suffix = ""; // no suffix for directory
		}else{
			throw new IOException("Unable to determine file suffix for report with type "+getMediaType());
		}
		// group reports by year of creation/extraction
		Path dest = createGroupedPath(archive.getDataDir(), getDataTimestamp(),getId()+suffix);
		log.info("Moving report data to "+dest);
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
	private String getPreferencesClob(){
		Properties props = new Properties();
		StringWriter writer = new StringWriter();
		try {
			props.store(writer, null);
		} catch (IOException e) {
			throw new AssertionError(); // should not happen
		}
		return writer.toString();
	}
	@Override
	public Map<String, String> getPreferences() {
		// lazy load
		if( this.prefs == null ){
			// get datasource from archive
			try( Connection dbc = archive.ds.getConnection() ){
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
				throw new UncheckedIOException(new IOException(e));
			} catch( IOException e ){
				throw new UncheckedIOException(e);
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

	@Override
	public Instant getCreatedTimestamp() {
		return this.createdTimestamp;
	}

	@Override
	public Status getStatus() {
		if( this.mediaType == null ){
			return Status.Waiting;
		}else if( this.mediaType.equals(MEDIATYPE_FAILURE_STACKTRACE) ){
			return Status.Failed;
		}else if( this.mediaType.equals(MEDIATYPE_INSUFFICIENT_DATA) ){
			return Status.InsufficientData;
		}else{
			return Status.Completed;
		}
	}
	private static LocalDateTime getLocalTime(Instant instant){
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	private void updateReportData(Connection dbc) throws SQLException{
		try( PreparedStatement ps = dbc.prepareStatement(updateReport) ){
			ps.setString(1, getMediaType());
			ps.setString(2, archive.getDataDir().relativize(getLocation()).toString());
			if( this.dataTimestamp == null ){
				ps.setTimestamp(3, null);
			}else{
				ps.setTimestamp(3, Timestamp.from(this.dataTimestamp));
			}
			if( this.prefs != null && this.prefs.size() > 0 ){
				ps.setString(4, getPreferencesClob());
			}else{
				ps.setString(4, null);
			}
			ps.setString(2, archive.getDataDir().relativize(getLocation()).toString());
			ps.setInt(5, this.id);
			ps.execute();
		}
	}
	private Path createGroupedPath(Path base, Instant timestamp, String file) throws IOException{
		int group = getLocalTime(timestamp).getYear();
		Path subdir = base.resolve(Integer.toString(group));
		Files.createDirectories(subdir); // make sure directory exists
		return subdir.resolve(file);
		
	}
	public void setFailed(Connection dbc, String description, Throwable cause) throws IOException, SQLException{
		if( cause instanceof InsufficientDataException ){
			this.mediaType = MEDIATYPE_INSUFFICIENT_DATA;
		}else{
			this.mediaType = MEDIATYPE_FAILURE_STACKTRACE;
		}
		// we don't have a data timestamp, use the created timestamp for path
		this.location = createGroupedPath(archive.getDataDir(), getCreatedTimestamp(),getId()+".txt");
		log.info("Writing report failure stacktrace to "+this.location);
		// TODO maybe write additional error output or warnings after the stack trace
		try( PrintWriter w = new PrintWriter(Files.newBufferedWriter(this.location, StandardOpenOption.CREATE_NEW)) ){
			// print description
			if( description != null ){
				w.println(description);
			}
			if( cause != null ){
				// empty line to separate the description
				if( description != null ){
					w.println();
				}
				// print stack trace
				cause.printStackTrace(w);
			}
			w.flush();
		}
		updateReportData(dbc);
	}

	public CompletableFuture<Void> createAsync(ReportManager manager) throws IOException{
		// generate report
		CompletableFuture<? extends GeneratedReport> f = manager.generateReport(this, null);
		return f.handle( (r,t) -> {
			if( r != null )try {
				archive.setReportResult(this.id, r);
			} catch (IOException e) {
				t = e;
			}
			if( t != null )try {
				log.log(Level.WARNING, "Report failed", t);
				archive.setReportFailure(this.id, null, t);
			} catch (IOException e1) {
				t.addSuppressed(e1);
				throw new IllegalStateException("Unable to write report error",t);
			}			
			return null;
		});
	}
	public void setData(Connection dbc, GeneratedReport report) throws IOException, SQLException {
		this.dataTimestamp = report.getDataTimestamp();
		// update preferences
		this.prefs = new HashMap<>(report.getPreferences());
		this.mediaType = report.getMediaType();
		moveGeneratedReportFiles(report.getLocation());
		updateReportData(dbc);
	}
}
