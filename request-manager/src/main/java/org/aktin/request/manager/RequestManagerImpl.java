package org.aktin.request.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.aktin.Preferences;
import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.broker.query.xml.QueryRequest;
import org.aktin.broker.request.RequestManager;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;
import org.aktin.broker.request.StatusChanged;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.request.Status;
import org.aktin.dwh.EMail;
import org.aktin.dwh.PreferenceKey;


@javax.ejb.Singleton
@javax.ejb.Startup
public class RequestManagerImpl extends RequestStoreImpl implements RequestManager{
	private static final Logger log = Logger.getLogger(RequestManagerImpl.class.getName());
	private static final long INITIAL_DELAY_MILLIS = 20*1000; // first execution after 20 seconds
	@Inject
	private Preferences prefs;

	@Inject
	private org.aktin.dwh.ImportSummary summ;

	@Inject
	private ResultUploader uploader;

	@Inject
	@StatusChanged
	private Event<RetrievedRequest> event;

	@Inject
	@EMail
	private Event<RetrievedRequest> emailEvent;

	@Resource
    private TimerService timer;

	private BrokerClient client;
	private boolean handshakeCompleted;


	public RequestManagerImpl() {

	}

	private void createIntervalTimer(){
		Duration interval;
		try{
			interval = Duration.parse(prefs.get(PreferenceKey.brokerIntervals));
		}catch( DateTimeParseException e ){
			throw new IllegalStateException("Unable to parse preference broker interval");
		}

		Timer t = timer.createIntervalTimer(INITIAL_DELAY_MILLIS, // start after 20 seconds
				interval.toMillis(), // repeat after interval
				new TimerConfig(null, false) // non persistent
		);
		log.info("Timer created, first callback in "+Duration.ofMillis(t.getTimeRemaining()));		
	}

	private Map<String,String> loadSoftwareVersions(){
		Map<String, String> versions = new HashMap<>();
		versions.put("dwh-api", PreferenceKey.class.getPackage().getImplementationVersion());
//		versions.put("dwh-db", LiquibaseWrapper.class.getPackage().getImplementationVersion());
		versions.put("java", System.getProperty("java.vendor")+"/"+System.getProperty("java.version"));
		// get application server version from TimerService implementation
		versions.put("j2ee-impl", timer.getClass().getPackage().getImplementationVersion());
		// get EAR version
		try {
			versions.put("ear", (String) (new InitialContext().lookup( "java:app/AppName")));
		} catch (NamingException e) {
			log.warning("Unable to get ear version via java:app/AppName");
		}
		return versions;
		// TODO find out application server name 
	}

	/**
	 * Handshake requests broker status and posts version information
	 * to the broker. Handshake is only required once after startup.
	 * @throws IOException communications error
	 */
	private void performBrokerHandshake() throws IOException{
		client.getBrokerStatus();
		client.postSoftwareVersions(loadSoftwareVersions());
	}

	@Override // TODO use preference instead of hard coded JNDI URI
	@Resource(lookup="java:jboss/datasources/AktinDS")
	protected void setDataSource(DataSource ds){
		super.setDataSource(ds);
	}

	private void initializeResultDirectory() throws IOException{
		// set result directory
		String dir = prefs.get(PreferenceKey.brokerDataPath);
		Objects.requireNonNull(dir, "Missing preference for "+PreferenceKey.brokerDataPath);
		Path path = Paths.get(dir);
		// make sure the directory exists
		Files.createDirectories(path);
		setResultDirectory(path);		
	}

	private void initializeBrokerClient(){
		String broker = prefs.get(PreferenceKey.brokerEndpointURI);
		if( broker == null || broker.trim().length() == 0 ){
			// no or empty broker URL, disable broker communication
			client = null;
			// there will be not timer callbacks
			return;
		}
		client = new BrokerClient(URI.create(broker));
		String apiKey = prefs.get(PreferenceKey.brokerEndpointKeys);
		client.setClientAuthenticator(HttpApiKeyAuth.newBearer(apiKey));
		// create timer to fetch requests
		createIntervalTimer();
	}

	@PostConstruct
	public void loadSchedule() {
		log.info("Initializing request manager");
		// load result directory
		try {
			initializeResultDirectory();
			reloadRequests();
			fireInterruptedEvents();
		} catch (IOException | SQLException | JAXBException e) {
			throw new IllegalStateException(e);
		}
		
		initializeBrokerClient();
	}

	private void reportStatusToBroker() throws IOException{
		if( false == handshakeCompleted ){
			// need to perform broker handshake only once after startup
			performBrokerHandshake();
			handshakeCompleted = true;
		}
		client.putMyResourceXml("stats", summ);
	}

	private void fetchNewRequests() throws IOException{
		// TODO what happens if the a request is deleted at the broker and it is still waiting locally? how do we update that info?
		// fetch requests
		List<RequestInfo> list = client.listMyRequests();
		Unmarshaller um;
		try{
			JAXBContext jc = JAXBContext.newInstance(QueryRequest.class);
			um = jc.createUnmarshaller();
		}catch( JAXBException e ){
			throw new IOException(e);
		}
		log.info("Broker lists "+list.size()+" requests");
		for( RequestInfo info : list ){
			if( info.nodeStatus == null || info.nodeStatus.size() == 0 ){
				log.info("Request "+info.getId()+" is new.");
				// new request
				// TODO load content
				try( Reader reader = client.getMyRequestDefinitionReader(info.getId(), QueryRequest.MEDIA_TYPE) ){
					QueryRequest req = (QueryRequest)um.unmarshal(reader);
					addNewRequest(req);
				}catch( IOException | JAXBException | SQLException e ){
					String message = "Failed to parse/store content for broker request "+info.getId();
					log.log(Level.SEVERE,message, e);
					client.postRequestFailed(info.getId(), message, e);
					client.deleteMyRequest(info.getId());
				}
				// set remote status to retrieved
				client.postRequestStatus(info.getId(), org.aktin.broker.xml.RequestStatus.retrieved);
				// during the next call, the request will be detected as already retrieved
			}else{
				// already retrieved
				// nothing to do
				log.info("Request "+info.getId()+" already retrieved.");
			}
		}
	}
	// timer will not be called if broker communication is disabled
	@Timeout
	private void timerCallback(Timer timer){
		log.info("Broker timer triggered. Next at "+timer.getNextTimeout());
		try {
			reportStatusToBroker();
			fetchNewRequests();
		}catch( ConnectException e ){
			log.severe("Unable to connect to broker "+prefs.get(PreferenceKey.brokerEndpointURI));
		}catch( UnknownHostException e ){
			log.severe("Unable to resolve broker hostname "+prefs.get(PreferenceKey.brokerEndpointURI));			
		}catch( FileNotFoundException e ){
			log.severe("Broker resource not found: "+e.getMessage());
		}catch( IOException e) {
			log.log(Level.SEVERE, "Broker communication failed", e);
		}
		// TODO go through requests and perform pending actions
	}

	@Override
	protected void afterRequestStatusChange(RequestImpl request, String description) {
		final RequestStatus status = request.getStatus();
		Annotation qualifier = new Status() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return Status.class;
			}
			@Override
			public RequestStatus value() {
				return status;
			}
		};
		event.select(qualifier).fire(request);

		reportStatusUpdatesToBroker(request, description);
	}


	/**
	 * Applies rules meant to run after the request was retrieved.
	 * E.g. Automatically accept some requests or reject all
	 * @param request request
	 */
	protected void applyPostRetrievalRules(RetrievedRequest request){
		// TODO load and apply rules
		// XXX for now, queue all requests
		try {
			request.setAutoSubmit(true);
			request.changeStatus(null, RequestStatus.Queued, "automatic accept");
		} catch (IOException e) {
			log.log(Level.SEVERE, "Unable to change status for request "+request.getRequestId(), e);
		}
	}

	private void fireInteraktionNotification(RetrievedRequest request){
		log.info("Interaction required for request "+request.getRequestId()+" status "+request.getStatus());
		emailEvent.fire(request);
	}

	private void postRequestStatus(int requestId, org.aktin.broker.xml.RequestStatus brokerStatus, String description){
		try {
			client.postRequestStatus(requestId, brokerStatus, null, description);
		} catch (IOException e) {
			// stack trace not needed for status report failures
			log.warning("Unable to report request status to broker: "+requestId+" -> "+brokerStatus+": "+e.toString());
		}
	}
	// automatically called by CDI event processing (somehow not allowed???)
	public void reportStatusUpdatesToBroker(RetrievedRequest request, String description){
//	public void reportStatusUpdatesToBroker(@Observes @StatusChanged RetrievedRequest request){
		log.info("Request "+request.getRequestId()+" status -> "+request.getStatus());
		int id = request.getRequestId();
		try{
			switch( request.getStatus() ){
			case Completed:
				// check if interaction is required
				if( request.hasAutoSubmit() ){
					// update status to sending
					log.info("No interaction for completed request "+request.getRequestId());
					request.changeStatus(null, RequestStatus.Sending, null);
				}else{
					// manual interaction required
					log.info("Interaction required for completed request "+request.getRequestId());
					fireInteraktionNotification(request);
					postRequestStatus(id, org.aktin.broker.xml.RequestStatus.interaction, description);
				}
				break;
			case Failed:
				// report failure message
				postRequestStatus(id, org.aktin.broker.xml.RequestStatus.failed, description);
				client.deleteMyRequest(id);
				break;
			case Processing:
				postRequestStatus(id, org.aktin.broker.xml.RequestStatus.processing, description);
				break;
			case Queued:
				postRequestStatus(id, org.aktin.broker.xml.RequestStatus.queued, description);
				break;
			case Rejected:
				postRequestStatus(id, org.aktin.broker.xml.RequestStatus.rejected, description);
				client.deleteMyRequest(id);
				break;
			case Retrieved:
				// retrieval was already reported after fetching
				// determine whether interaction is required
				applyPostRetrievalRules(request);
				if( request.getStatus() == RequestStatus.Retrieved ){
					// rules didn't change the status. manual interaction required
					fireInteraktionNotification(request);
					postRequestStatus(id, org.aktin.broker.xml.RequestStatus.interaction, description);
				}
				break;
			case Seen:
				break;
			case Sending:
				uploader.accept(request);
				break;
			case Submitted:
				postRequestStatus(id, org.aktin.broker.xml.RequestStatus.completed, description);
				client.deleteMyRequest(id);
				break;
			default:
				break;
			
			}
		}catch( IOException e ){
			log.log(Level.SEVERE,"Unable to report request status to broker", e);
		}
	}

	@Override
	public List<? extends RetrievedRequest> getQueryRequests(int queryId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
