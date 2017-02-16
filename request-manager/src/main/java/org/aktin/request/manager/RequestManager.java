package org.aktin.request.manager;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.aktin.Module;
import org.aktin.Preferences;
import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.broker.xml.SoftwareModule;
import org.aktin.dwh.PreferenceKey;


@javax.ejb.Singleton
@javax.ejb.Startup
public class RequestManager extends Module {
	private static final Logger log = Logger.getLogger(RequestManager.class.getName());
	private static final long INITIAL_DELAY_MILLIS = 20*1000; // first execution after 20 seconds
	@Inject
	private Preferences prefs;

	@Resource
    private TimerService timer;

	private BrokerClient client;
	private long startupTimestamp;


	public RequestManager() {
		startupTimestamp = System.currentTimeMillis();
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

	private void initializeBrokerClient(){
		URI uri = URI.create(prefs.get(PreferenceKey.brokerEndpointURI));
		client = new BrokerClient(uri);
		String apiKey = prefs.get(PreferenceKey.brokerEndpointKeys);
		client.setClientAuthenticator(HttpApiKeyAuth.newBearer(apiKey));
	}

	@PostConstruct
	public void loadSchedule() {
		log.info("Initializing request manager");
		initializeBrokerClient();
		createIntervalTimer();
		
	}

	private void reportStatusToBroker(){
		try {
			client.getBrokerStatus();
			client.postMyStatus(startupTimestamp, new SoftwareModule("dwh-api", PreferenceKey.class.getPackage().getImplementationVersion()));
		}catch( ConnectException e ){
			log.severe("Unable to connect to broker "+prefs.get(PreferenceKey.brokerEndpointURI));
		}catch( IOException e) {
			log.log(Level.SEVERE, "Broker communication failed", e);
		}
	}

	@Timeout
	private void timerCallback(Timer timer){
		log.info("Broker timer triggered.");
		log.info("Next scheduled timer at "+timer.getNextTimeout());
		reportStatusToBroker();
	}


}
