package org.aktin.request.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;

import org.aktin.broker.query.xml.QueryRequest;
import org.aktin.broker.request.RequestStatus;

abstract class RequestStoreImpl {
	private static final Logger log = Logger.getLogger(RequestStoreImpl.class.getName());

	private DataSource ds;
	private Path resultDir;
//	private Path intermediateDir;
	private List<RequestImpl> requests;
	
	public RequestStoreImpl(){
		requests = new ArrayList<>();
	}

	RequestStoreImpl(DataSource ds, Path resultDir){
		this();
		this.ds = ds;
		this.resultDir = resultDir;
	}

	protected void setDataSource(DataSource ds){
		this.ds = ds;
	}

	protected void setResultDirectory(Path resultDir){
		this.resultDir = resultDir;
	}
	protected void reloadRequests() throws SQLException, JAXBException{
		requests.clear();
		try( Connection dbc = ds.getConnection() ){
			RequestImpl.loadAll(this, dbc, requests::add);
		}
		log.info("Loaded "+requests.size()+" requests");
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
				afterRequestStatusChange(req, null);
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
	/**
	 * Create a directory path for storing intermediate
	 * files. This will be used for executions with multiple processing steps.
	 * @param impl request implementation
	 * @param stepNo step number to differentiate between multiple intermediate stages
	 * @return Path where intermediate files can be stored for the query
	 * @throws IOException 
	 */
	protected Path createIntermediatePath(RequestImpl impl, int stepNo) throws IOException {
		return Files.createTempDirectory("request-"+impl.getRequestId()+"-"+stepNo);
	}
	
	protected Connection getConnection() throws SQLException{
		return ds.getConnection();
	}
	protected RequestImpl addNewRequest(QueryRequest request) throws SQLException{
		RequestImpl r = new RequestImpl(this, request);
		long timestamp = System.currentTimeMillis();
		try( Connection dbc = getConnection() ){
			dbc.setAutoCommit(true);
			r.writeStatusChange(dbc, timestamp, null, RequestStatus.Retrieved, null);
			r.insertIntoDatabase(dbc);
		}
		// append to list
		requests.add(r);
		log.info("Request "+request.getId()+" added. Firing status event..");
		// fire status change event: null -> Retrieved
		afterRequestStatusChange(r, null);
		return r;
	}
	/**
	 * Called after the request status is changed.
	 * @param request request
	 * @param description description
	 */
	protected abstract void afterRequestStatusChange(RequestImpl request, String description);
}
