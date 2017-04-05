package org.aktin.request.manager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;

import org.aktin.broker.query.xml.QueryRequest;
import org.aktin.broker.request.RequestStatus;

abstract class RequestStoreImpl {

	private DataSource ds;
	private Path resultDir;
	private List<RequestImpl> requests;
	
	public RequestStoreImpl(){
		requests = new ArrayList<>();
	}

	RequestStoreImpl(DataSource ds, Path resultDir){
		this();
		this.ds = ds;
		this.resultDir = resultDir;
	}

	// TODO methods setDataSource, setResultDir

	protected void reloadRequests() throws SQLException, JAXBException{
		requests.clear();
		try( Connection dbc = ds.getConnection() ){
			RequestImpl.loadAll(this, dbc, requests::add);
		}
	}

	public List<RequestImpl> getRequests(){
		return requests;
	}
	/**
	 * Fires status change events for events which do not require user interaction.
	 * This will kickstart the request processing queue.
	 * <p>
	 * This is necessary because the server might have been stopped during processing
	 * queued events. In this case, the server will start with some events in the
	 * queued status.
	 * </p>
	 * Call this method after loading the requests from the database
	 */
	protected void fireInterruptedEvents(){
		for( RequestImpl req : requests ){
			//
			if( req.getStatus() == RequestStatus.Queued ){
				afterRequestStatusChange(req);
			}
		}
	}
	protected Path getResultDir(){
		return resultDir;
	}

	protected String createResultPath(String mediaType, RequestImpl impl){
		// generate a result path name
		// TODO better name
		// TODO suffix by media type
		return Integer.toString(impl.getRequestId());
	}
	protected Connection getConnection() throws SQLException{
		return ds.getConnection();
	}
	protected RequestImpl addNewRequest(QueryRequest request) throws SQLException{
		RequestImpl r = new RequestImpl(this, request);
		long timestamp = System.currentTimeMillis();
		try( Connection dbc = getConnection() ){
			dbc.setAutoCommit(true);
			r.changeStatus(dbc, timestamp, null, RequestStatus.Retrieved, null);
			r.insertIntoDatabase(dbc);
		}
		// append to list
		requests.add(r);
		// fire status change event: null -> Retrieved
		afterRequestStatusChange(r);
		return r;
	}
	/**
	 * Called after the request status is changed.
	 * @param request request
	 */
	protected abstract void afterRequestStatusChange(RequestImpl request);
}
