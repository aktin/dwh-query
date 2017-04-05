package org.aktin.request.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.function.Consumer;

import javax.activation.DataSource;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.aktin.broker.query.xml.QueryRequest;
import org.aktin.broker.request.ActionLogEntry;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;

public class RequestImpl implements RetrievedRequest, DataSource{

	private RequestStoreImpl store;
	private int requestId;
	private int queryId;
	private RequestStatus status;
	private boolean autoSubmit;
	private String resultPath;
	private String resultType;
	private QueryRequest request;
	private long lastActionTime;

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
	 * @param store
	 * @param request
	 * @return
	 */
	RequestImpl(RequestStoreImpl store, QueryRequest request){
		this(store);
		this.request = request;
		requestId = request.getId();
		queryId = request.getQuery().id;
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
			ps.setInt(2, queryId);
			StringWriter w = new StringWriter();
			JAXB.marshal(request, w);
			ps.setString(3, w.toString());
			ps.setBoolean(4, autoSubmit);
			ps.setString(5, status.name());
			ps.executeUpdate();
		}
	}
	protected static void loadAll(RequestStoreImpl store, Connection dbc, Consumer<RequestImpl> action) throws SQLException, JAXBException{
		JAXBContext jc = JAXBContext.newInstance(QueryRequest.class);
		Unmarshaller um = jc.createUnmarshaller();
		try( Statement st = dbc.createStatement();
				ResultSet rs = st.executeQuery(
						"SELECT broker_request_id, broker_query_id, auto_submit, request_xml,"
						+ " status, result_type, result_path, display"
						+ " FROM broker_requests WHERE display != 'H'") ){
			while( rs.next() ){
				RequestImpl r = new RequestImpl(store);
				// load other data
				r.requestId = rs.getInt(1);
				r.queryId = rs.getInt(2);
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
				// TODO use display type
				action.accept(r);
			}
		}
	}
	@Override
	public int getRequestId() {
		return requestId;
	}

	@Override
	public int getQueryId() {
		return queryId;
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
	public Iterable<ActionLogEntry> getActionLog() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAutoSubmit() {
		return autoSubmit;
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
	public DataSource getResultData() throws IOException {
		if( resultType == null ){
			return null;
		}
		return this;
	}

	protected void changeStatus(Connection dbc, long timestamp, String userId, RequestStatus newStatus, String description) throws SQLException{		
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
			changeStatus(dbc, System.currentTimeMillis(), userId, newStatus, description);
		} catch (SQLException e) {
			throw new IOException("Unable to write action to database", e);
		}
		// fire event
		store.afterRequestStatusChange(this);
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

}
