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
import java.util.function.Consumer;
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
import org.aktin.broker.request.InteractionPreset;
import org.aktin.broker.request.RequestManager;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;
import org.aktin.broker.request.StatusChanged;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.request.Status;
import org.aktin.dwh.EmailService;
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

	private Consumer<RetrievedRequest> uploader;

	@Inject
	@StatusChanged
	private Event<RetrievedRequest> event;

	@Resource
    private TimerService timer;

	@Inject
	private EmailService email;

	private BrokerClient client;
	private InteractionPreset interaction;
	private boolean handshakeCompleted;

	public RequestManagerImpl() {

	}

	public RequestManagerImpl(Preferences prefs){
		this.prefs = prefs;
	}

	@Inject
	void setResultUploader(ResultUploader uploader){
		this.uploader = uploader;
	}
	/**
	 * Set the result uploader. The upload is usually asynchronously.
	 * Upon completion, {@link RetrievedRequest#changeStatus(String, RequestStatus, String)} should be called
	 * to set the status to either {@link RequestStatus#Submitted} or {@link RequestStatus#Failed}.
	 * @param uploader result uploader
	 */
	public void setResultUploader(Consumer<RetrievedRequest> uploader){
		this.uploader = uploader;
	}

	private void createIntervalTimer(){
		Duration interval;
		try{
			interval = Duration.parse(prefs.get(PreferenceKey.brokerIntervals));
		}catch( DateTimeParseException e ){
			throw new IllegalStateException("Unable to parse preference broker interval");
		}

		// some unit tests may not have a timer service available
		// during production, the TimerService resource is always available
		if( timer != null ){
			// use timer service
			Timer t = timer.createIntervalTimer(INITIAL_DELAY_MILLIS, // start after 20 seconds
					interval.toMillis(), // repeat after interval
					new TimerConfig(null, false) // non persistent
			);
			log.info("Timer created, first callback in "+Duration.ofMillis(t.getTimeRemaining()));					
		}else{
			log.warning("Timer service not available. Running without automatic request updates!");
		}
	}

	private Map<String,String> loadSoftwareVersions(){
		Map<String, String> versions = new HashMap<>();
		versions.put("dwh-api", Objects.toString(PreferenceKey.class.getPackage().getImplementationVersion()));
//		versions.put("dwh-db", LiquibaseWrapper.class.getPackage().getImplementationVersion());
		versions.put("java", System.getProperty("java.vendor")+"/"+System.getProperty("java.version"));
		// get application server version from TimerService implementation
		String ver;
		if( timer != null ){
			ver =  timer.getClass().getPackage().getImplementationVersion();
		}else{
			ver = "undefined";
		}
		versions.put("j2ee-impl", ver);

		// get EAR version
		try {
			ver = null;
			ver = (String) (new InitialContext().lookup( "java:app/AppName"));
		} catch (NamingException e) {
			log.warning("Unable to get ear version via java:app/AppName");
		}
		if( ver == null ){
			ver = "undefined";
		}
		versions.put("ear", ver);
		System.out.println(versions);
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
	public void setDataSource(DataSource ds){
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

	private void loadRequestInteractionOverrides(){
		// determine interaction override
		String ix = prefs.get("broker.request.interaction"); // XXX use enum
		if( ix == null ){
			// default to user interaction
			interaction = InteractionPreset.USER;
		}else{
			switch( ix ){
			case "user":
				interaction = InteractionPreset.USER;
				break;
			case "non-interactive-reject":
				interaction = InteractionPreset.NON_INTERACTIVE_REJECT;
				break;
			case "non-interactive-allow":
				interaction = InteractionPreset.NON_INTERACTIVE_ALLOW;
				break;
			default:
				throw new IllegalArgumentException("Unsupported value for preference broker.request.interaction: "+ix);
			}
		}
		log.info("Broker request interaction: "+interaction);
	}
	private void initializeBrokerClient(){
		String broker = prefs.get(PreferenceKey.brokerEndpointURI);
		if( broker == null || broker.trim().length() == 0 ){
			// no or empty broker URL, disable broker communication
			client = null;
			// there will be not timer callbacks
			log.warning("No broker endpoint URI defined, running without without broker.");
			return;
		}
		client = new BrokerClient(URI.create(broker));
		String apiKey = prefs.get(PreferenceKey.brokerEndpointKeys);
		client.setClientAuthenticator(HttpApiKeyAuth.newBearer(apiKey));
		// create timer to fetch requests
		createIntervalTimer();
	}

	@PostConstruct
	public void loadData() {
		log.info("Initializing request manager");
		loadRequestInteractionOverrides();
		// load result directory
		try {
			initializeResultDirectory();
			reloadRequests();
			initializeBrokerClient();
			fireInterruptedEvents();
		} catch (IOException | SQLException | JAXBException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Send status update to broker
	 * @throws IOException IO error
	 */
	public void reportStatusToBroker() throws IOException{
		if( false == handshakeCompleted ){
			// need to perform broker handshake only once after startup
			performBrokerHandshake();
			handshakeCompleted = true;
		}
		if( summ != null ){
			client.putMyResourceXml("stats", summ);			
		}
	}

	/**
	 * Fetch new requests from the configured broker endpoint
	 * @throws IOException IO error
	 */
	public void fetchNewRequests() throws IOException{
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
					// handle null reader
					if( reader == null ){
						throw new IOException("No request definition found for "+info.getId()+", "+QueryRequest.MEDIA_TYPE);
					}
					QueryRequest req = (QueryRequest)um.unmarshal(reader);
					if( req == null ){
						throw new JAXBException("XML unmarshalling returned null");
					}
					addNewRequest(req);
				}catch( IOException | JAXBException | SQLException e ){ // catch throwable here?
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
		// request processing is done via status change callbacks
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
		// in application server, event is guaranteed to be non null
		// however unit tests don't have 
		if( event != null ){
			event.select(qualifier).fire(request);
		}

		reportStatusUpdatesToBroker(request, description);
	}


	/**
	 * Applies rules meant to run after the request was retrieved.
	 * E.g. Automatically accept some requests or reject all
	 * @param request request
	 */
	protected void applyPostRetrievalRules(RetrievedRequest request){
		// check for interaction overrides
		try {
			switch( interaction ){
			case USER:
				// TODO load and apply user defined rules
				break;
			case NON_INTERACTIVE_ALLOW:
				request.setAutoSubmit(true);
				request.changeStatus(null, RequestStatus.Queued, "automatic accept");
				break;
			case NON_INTERACTIVE_REJECT:
				request.changeStatus(null, RequestStatus.Rejected, "automatic reject");
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, "Unable to change status for request "+request.getRequestId(), e);
		}
	}

	/**
	 * Report a change of status to the broker and optionally via email to user
	 * @param request request after the status change
	 * @param brokerStatus matching broker status. E.g. {@code interaction} if interaction is required
	 * @param description description
	 */
	private void reportRequestStatusChanged(RetrievedRequest request, org.aktin.broker.xml.RequestStatus brokerStatus, String description){
		int requestId = request.getRequestId();
		try {
			client.postRequestStatus(requestId, brokerStatus, null, description);
		} catch (IOException e) {
			// stack trace not needed for status report failures
			log.warning("Unable to report request status to broker: "+requestId+" -> "+brokerStatus+": "+e.toString());
		}
		// send notifications
		// TODO load email notification settings (how often / on what status changes to send emails)
		if( brokerStatus == org.aktin.broker.xml.RequestStatus.interaction ){
			// interaction required
			log.info("Interaction required for request "+request.getRequestId()+" w/ status="+request.getStatus());
			StringBuilder body = new StringBuilder();
			body.append("Sehr geehrte Damen und Herren,\n\n");
			body.append("eine Datenanfrage in Ihrem AKTIN Data Warehouse erfordert Ihre Aufmerksamkeit.\n");

			body.append("Bitte loggen Sie sich in Ihrem AKTIN Data Warehouse ein,\n");
			body.append("um diese Anfrage zu bearbeiten.\n");
			String url = prefs.get(PreferenceKey.serverUrl)+"aktin/admin/#/request/"+request.getRequestId();
			body.append("Link: ").append(url).append("\n\n");

			switch( request.getStatus() ){
			case Retrieved:
			case Seen:
				body.append("Die folgende Anfrage ist neu eingegangen und wartet\n");
				body.append("auf Ihre Freigabe um die Auswertung durchzuführen:\n");
				break;
			case Completed:
				body.append("Die folgende Anfrage wurde erfolgreich ausgeführt\n");
				body.append("und wartet auf Ihre Freigabe zur Übermittlung der Ergebnisse:\n");
				break;
			default:
				break;
			}
			body.append('\n');
			body.append("Titel: ").append(request.getRequest().getQuery().title).append('\n');
			String desc = request.getRequest().getQuery().description;
			if( desc != null && desc.length() > 0 ){
				body.append("Beschreibung:\n");
				body.append(desc);
				body.append("\n\n");
			}
			// TODO more info for query
			body.append("Link: ").append(url).append("\n");
	
			
			body.append("\nMit freundlichen Grüßen,\n");
			body.append("Ihr lokaler AKTIN-Server\n");
			
			try{
				email.sendEmail("[AKTIN] Aktion erforderlich für Datenanfrage "+request.getRequestId(), body.toString());
			}catch( IOException e ){
				log.log(Level.SEVERE, "Unable to send email", e);
			}
		}
	}

	// automatically called by CDI event processing (somehow not allowed???)
	public void reportStatusUpdatesToBroker(RetrievedRequest request, String description){
//	public void reportStatusUpdatesToBroker(@Observes @StatusChanged RetrievedRequest request){
		log.info("Request "+request.getRequestId()+" status -> "+request.getStatus());
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
					reportRequestStatusChanged(request, org.aktin.broker.xml.RequestStatus.interaction, description);
				}
				break;
			case Failed:
				// report failure message
				reportRequestStatusChanged(request, org.aktin.broker.xml.RequestStatus.failed, description);
				client.deleteMyRequest(request.getRequestId());
				break;
			case Processing:
				reportRequestStatusChanged(request, org.aktin.broker.xml.RequestStatus.processing, description);
				break;
			case Queued:
				reportRequestStatusChanged(request, org.aktin.broker.xml.RequestStatus.queued, description);
				break;
			case Rejected:
				reportRequestStatusChanged(request, org.aktin.broker.xml.RequestStatus.rejected, description);
				client.deleteMyRequest(request.getRequestId());
				break;
			case Retrieved:
				// retrieval status was already reported after fetching
				// determine whether interaction is required
				applyPostRetrievalRules(request);
				if( request.getStatus() == RequestStatus.Retrieved ){
					// rules didn't change the status. manual interaction required
					reportRequestStatusChanged(request, org.aktin.broker.xml.RequestStatus.interaction, description);
				}else{
					log.info("Post retrieval rules changed status for request "+request.getRequestId()+" to "+request.getStatus());
				}
				break;
			case Seen:
				break;
			case Sending:
				uploader.accept(request);
				break;
			case Submitted:
				reportRequestStatusChanged(request, org.aktin.broker.xml.RequestStatus.completed, description);
				client.deleteMyRequest(request.getRequestId());
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

	@Override
	public InteractionPreset getInteractionPreset() {
		return interaction;
	}
	@Override
	public void forEachRequest(Consumer<RetrievedRequest> action) {
		getRequests().forEach( action );
		
	}

}
