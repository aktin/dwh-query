package org.aktin.request.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.aktin.Preferences;
import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.dwh.PreferenceKey;


@javax.ejb.Singleton
@javax.ejb.Startup
public class RequestManagerImpl {
	private static final Logger log = Logger.getLogger(RequestManagerImpl.class.getName());
	private static final long INITIAL_DELAY_MILLIS = 20*1000; // first execution after 20 seconds
	@Inject
	private Preferences prefs;

	@Inject
	private org.aktin.dwh.ImportSummary summ;

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
		// TODO fetch requests, post events		
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


}
