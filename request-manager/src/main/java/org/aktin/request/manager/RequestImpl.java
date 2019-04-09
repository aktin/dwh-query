package org.aktin.request.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

import javax.activation.DataSource;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.aktin.broker.query.xml.QueryRequest;
import org.aktin.broker.request.ActionLogEntry;
import org.aktin.broker.request.Marker;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;

public class RequestImpl implements RetrievedRequest, DataSource{
//	private static final Logger log = Logger.getLogger(RequestManagerImpl.class.getName());

	private RequestStoreImpl store;
	private int requestId;
	private RequestStatus status;
	private boolean autoSubmit;
	private String resultPath;
	private String resultType;
	private QueryRequest request;
	private long lastActionTime;
	private Marker marker;
	private List<ActionLogEntry> statusLog;

	private RequestImpl(RequestStoreImpl store){
		this.store = store;
	}

	/**
	 * Create new request from unmarshalled query request.
	 * This does not set the lastActionTime and neither writes
	 * the request to the database nor fires any events.
	 * <p>
	 * After calling this constructor, make sure to change
	 * the status via {@link #changeStatus(Connection, long, String, RequestStatus, String)}, 
	 * write to data base and fire an event.
	 * </p>
	 * @param store request store
	 * @param request query data
	 */
	RequestImpl(RequestStoreImpl store, QueryRequest request){
		this(store);
		Objects.requireNonNull(request);
		this.request = request;
		requestId = request.getId();
		// defaults to empty result
		resultPath = null;
		resultType = null;
		// auto submit defaults to false
		autoSubmit = false;
	}

	protected void insertIntoDatabase(Connection dbc) throws SQLException{
		// TODO insert all fields
		try( PreparedStatement ps = dbc.prepareStatement("INSERT INTO broker_requests(broker_request_id, broker_query_id, request_xml, auto_submit, status) VALUES (?,?,?,?,?)") ){
			ps.setInt(1, requestId);
			Integer queryId = getRequest().getQueryId();
			if( queryId != null ){
				ps.setInt(2, queryId);
			}else{
				ps.setNull(2, Types.INTEGER);
			}
			StringWriter w = new StringWriter();
			JAXB.marshal(request, w);
			ps.setString(3, w.toString());
			ps.setBoolean(4, autoSubmit);
			ps.setString(5, status.name());
			ps.executeUpdate();
		}
	}

	/**
	 * Used as endpoint for digest calculation filter stream.
	 */
	private static final class NullOutputStream extends OutputStream{
		@Override
		public void write(int b) throws IOException {}
	}
	protected static byte[] calculateQuerySignature(QueryRequest request, String algo) throws NoSuchAlgorithmException, IOException{
		Marshaller m;
		try {
			m = createJAXBContext().createMarshaller();
		} catch (JAXBException e) {
			throw new IOException("JAXB error", e);
		}
		MessageDigest digest = MessageDigest.getInstance(algo);
		try( DigestOutputStream ds = new DigestOutputStream(new NullOutputStream(), digest) ){
			m.marshal(request.getQuery(), ds);			
		} catch (JAXBException e) {
			throw new IOException("JAXB error", e);
		}
		return digest.digest();
	}
	protected static JAXBContext createJAXBContext() throws JAXBException{
		return JAXBContext.newInstance(QueryRequest.class);
	}
	protected static void loadAll(RequestStoreImpl store, Connection dbc, Consumer<RequestImpl> action) throws SQLException, JAXBException{
		JAXBContext jc = createJAXBContext();
		Unmarshaller um = jc.createUnmarshaller();
		try( Statement st = dbc.createStatement();
				ResultSet rs = st.executeQuery(
						"SELECT r.broker_request_id, r.broker_query_id, r.auto_submit, r.request_xml,"
						+ " r.status, r.result_type, r.result_path, r.display, MAX(l.timestamp)"
						+ " FROM broker_requests r"
						+ "  LEFT OUTER JOIN request_action_log l ON r.broker_request_id=l.broker_request_id"
						+ " GROUP BY r.broker_request_id, r.broker_query_id, r.auto_submit, r.request_xml, r.status, r.result_type, r.result_path, r.display"
						+ " ORDER BY r.broker_request_id DESC") ){
			while( rs.next() ){
				RequestImpl r = new RequestImpl(store);
				// load other data
				r.requestId = rs.getInt(1);
				r.autoSubmit = rs.getBoolean(3);
				r.request = (QueryRequest)um.unmarshal(new StringReader(rs.getString(4)));
// somehow, the following does not work with hsqldb
//				try( Reader reader = rs.getClob(4).getCharacterStream() ){
//					r.request = (QueryRequest)um.unmarshal(reader);
//				} catch (IOException e) {
//					throw new SQLException("Unable to read from XML CLOB");
//				}
				r.status = RequestStatus.valueOf(rs.getString(5));
				r.resultType = rs.getString(6);
				r.resultPath = rs.getString(7);
				// use display type
				r.marker = parseMarker(rs.getString(8));
				Timestamp ts = rs.getTimestamp(9);
				if( ts != null ) {
					r.lastActionTime = ts.getTime();
				}else {
					// does not have a valid timestamp, there was no previous action
				}
				action.accept(r);
			}
		}
	}
	private static Marker parseMarker(String displayChar){
		if( displayChar == null ){
			return null;
		}else if( displayChar.equals("H") ){
			return Marker.HIDDEN;
		}else if( displayChar.equals("S") ){
			return Marker.STARRED;
		}else{
			// TODO log warning
			return null;
		}
	}
	private static String compileMarker(Marker marker){
		if( marker == null ){
			return "";
		}else switch( marker ){
		case HIDDEN:
			return "H";
		case STARRED:
			return "S";
		default:
			// TODO log warning
			return null;
		}
	}
	@Override
	public int getRequestId() {
		return requestId;
	}

	@Override
	public QueryRequest getRequest() {
		return request;
	}

	@Override
	public RequestStatus getStatus() {
		return status;
	}

	@Override
	public Iterable<ActionLogEntry> getActionLog() throws IOException{
		if( statusLog == null ) {
			// load log
			try( Connection dbc = store.getConnection();
					Statement st = dbc.createStatement() ){
				
				ResultSet rs = st.executeQuery(
						"SELECT timestamp, user_id, old_status, new_status, description"
						+ " FROM request_action_log"
						+ "  WHERE broker_request_id="+requestId
						+ " ORDER BY timestamp ASC");
				this.statusLog = new ArrayList<>();
				while( rs.next() ){
					RequestStatus oldStatus = null;
					if( rs.getString(3) != null ) {
						oldStatus = RequestStatus.valueOf(rs.getString(3));
					}
					statusLog.add(new LogEntryImpl(rs.getString(2), rs.getTimestamp(1).getTime(), oldStatus, RequestStatus.valueOf(rs.getString(4)), rs.getString(5)));

				}
				rs.close();
			}catch( SQLException e ) {
				statusLog = null;
				throw new IOException("Unable to load status log entries for request "+requestId, e);
			}
		}
		return statusLog;
	}

	@Override
	public boolean hasAutoSubmit() {
		return autoSubmit;
	}


	@Override
	public DataSource getResultData() throws IOException {
		if( resultType == null ){
			return null;
		}
		return this;
	}

	protected void writeStatusChange(Connection dbc, long timestamp, String userId, RequestStatus newStatus, String description) throws SQLException{		
		Objects.requireNonNull(newStatus);
		try( PreparedStatement ps = dbc.prepareStatement("INSERT INTO request_action_log(broker_request_id, timestamp, user_id, old_status, new_status, description)VALUES(?,?,?,?,?,?)") ){
			ps.setInt(1, requestId);
			ps.setTimestamp(2, new Timestamp(timestamp));
			ps.setString(3, userId);
			ps.setString(4, (this.status==null)?null:this.status.name());
			ps.setString(5, newStatus.name());
			ps.setString(6, description);
			ps.executeUpdate();
		}
		try( PreparedStatement ps = dbc.prepareStatement("UPDATE broker_requests SET status=? WHERE broker_request_id=?") ){
			ps.setString(1, newStatus.name());
			ps.setInt(2, requestId);
			ps.executeUpdate();
		}
		// update log, if loaded previously
		if( this.statusLog != null ) {
			// log entries already loaded, add new entry to the end
			this.statusLog.add(new LogEntryImpl(userId, timestamp, this.status, newStatus, description));
		}

		this.status = newStatus;
		this.lastActionTime = timestamp;
	}
	@Override
	public void changeStatus(String userId, RequestStatus newStatus, String description)throws IOException {
		RequestStatus oldStatus = getStatus();
		if( oldStatus == newStatus ){
			// no change
			return;
		}
		try( Connection dbc = store.getConnection() ){
			dbc.setAutoCommit(true);
			writeStatusChange(dbc, System.currentTimeMillis(), userId, newStatus, description);
		} catch (SQLException e) {
			throw new IOException("Unable to write action to database", e);
		}
		// fire event
		store.afterRequestStatusChange(this, description);
	}
	

	private void updateResultInfo(Connection dbc, String resultType, String resultPath) throws SQLException{
		try( PreparedStatement ps = dbc.prepareStatement("UPDATE broker_requests SET result_type=?, result_path=? WHERE broker_request_id=?") ){
			ps.setString(1, resultType);
			ps.setString(2, resultPath);
			ps.setInt(3, requestId);
			ps.executeUpdate();
		}
	}

	@Override
	public void createResultData(String mediaType) throws IOException {
		Objects.requireNonNull(mediaType);
		this.resultType = mediaType;
		this.resultPath = store.createResultPath(mediaType, this);
		// write to database
		try( Connection dbc = store.getConnection() ){
			dbc.setAutoCommit(true);
			updateResultInfo(dbc, this.resultType, this.resultPath);
		} catch (SQLException e) {
			throw new IOException("Unable to update result columns in database", e);
		}
	}

	@Override
	public void removeResultData() throws IOException {
		if( resultType == null ){
			// nothing to do
			return;
		}
		// delete data file
		Files.deleteIfExists(store.getResultDir().resolve(resultPath));
		// set path, type to NULL in database
		try( Connection dbc = store.getConnection() ){
			dbc.setAutoCommit(true);
			updateResultInfo(dbc, null, null);
		} catch (SQLException e) {
			throw new IOException("Unable to update result columns in database", e);
		}
		this.resultPath = null;
		this.resultType = null;
	}
	@Override
	public long getLastActionTimestamp() {
		return lastActionTime;
	}

	@Override
	public void setProcessing(Map<String, String> properties, String stepName, int stepNo, int numSteps) throws IOException {
		if( this.status == RequestStatus.Processing ) {
			return; // only first set of properties is stored
			// server 
		}
		// store properties in status change description
		StringWriter w = new StringWriter();
		Properties props = new Properties();
		props.putAll(properties);
		props.store(w, "Processing properties");
		
		try( Connection dbc = store.getConnection() ){
			dbc.setAutoCommit(true);
			writeStatusChange(dbc, System.currentTimeMillis(), null, RequestStatus.Processing, w.toString());
		} catch (SQLException e) {
			throw new IOException("Unable to write action to database", e);
		}
		// fire event
		store.afterRequestStatusChange(this, w.toString());
	}

	@Override
	public void setAutoSubmit(boolean autoSubmit) throws IOException {
		if (this.autoSubmit != autoSubmit) {
			this.lastActionTime = System.currentTimeMillis();
		}
		this.autoSubmit = autoSubmit;
		// write to database
		try( Connection dbc = store.getConnection() ){
			dbc.setAutoCommit(true);
			PreparedStatement ps = dbc.prepareStatement("UPDATE broker_requests SET auto_submit=? WHERE broker_request_id=?");
			ps.setBoolean(1, autoSubmit);
			ps.setInt(2, requestId);
			ps.executeUpdate();
			
			// TODO find a way to update last modified timestamp for request if autoSubmit was changed. used for the GUI
			
		} catch (SQLException e) {
			throw new IOException("Unable to write action to database", e);
		}
	}

	@Override
	public Marker getMarker() {
		return marker;
	}

	@Override
	public void setMarker(Marker newMarker) throws IOException {
		if( Objects.equals(this.marker, newMarker) ){
			// nothing to do
			return;
		}
		// save changes
		try( Connection dbc = store.getConnection();
				PreparedStatement ps = dbc.prepareStatement("UPDATE broker_requests SET display=? WHERE broker_request_id=?") ){
			ps.setString(1, compileMarker(newMarker));
			ps.setInt(2, requestId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new IOException(e);
		}
		this.marker = newMarker;
		
	}

	@Override // DataSource
	public InputStream getInputStream() throws IOException {
		if( resultPath == null ){
			return null;
		}
		return Files.newInputStream(store.getResultDir().resolve(resultPath));
	}

	@Override // DataSource
	public OutputStream getOutputStream() throws IOException {
		if( resultPath == null ){
			return null;
		}
		return Files.newOutputStream(store.getResultDir().resolve(resultPath), StandardOpenOption.CREATE_NEW);
	}

	@Override // DataSource
	public String getContentType() {
		return resultType;
	}

	@Override // DataSource
	public String getName() {
		if( resultPath == null ){
			return null;
		}
		return store.getResultDir().resolve(resultPath).getFileName().toString();
	}

	@Override
	public Path createIntermediateDirectory(int stepNo) throws IOException {
		return store.createIntermediatePath(this, stepNo);
	}


}
