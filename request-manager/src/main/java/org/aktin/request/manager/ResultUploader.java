package org.aktin.request.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.aktin.Preferences;
import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.BrokerClient.OutputWriter;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;
import org.aktin.dwh.PreferenceKey;

@Singleton
public class ResultUploader implements Consumer<RetrievedRequest>{
	private static final Logger log = Logger.getLogger(ResultUploader.class.getName());

	private BrokerClient client;
	private Executor executor;
	private int uploadBufferSize;

	@Inject
	private Preferences prefs;

	public ResultUploader(){
		this.uploadBufferSize = 1024*1024*16;
	}

	@Resource(lookup="java:comp/DefaultManagedExecutorService")
	public void setExecutor(Executor executor){
		this.executor = executor;
	}

	@PostConstruct
	private void initializeAggregatorClient(){
		String broker = prefs.get(PreferenceKey.brokerEndpointURI);
		if( broker == null || broker.trim().length() == 0 ){
			// no or empty broker URL, disable broker communication
			client = null;
			// there will be no uploads
			log.warning("No broker configured, there will be no aggregator uploads");
			return;
		}
		client = new BrokerClient(URI.create(broker));
		String apiKey = prefs.get(PreferenceKey.brokerEndpointKeys);
		client.setClientAuthenticator(HttpApiKeyAuth.newBearer(apiKey));
	}


	@Override
	public void accept(RetrievedRequest request){
		if( client == null ){
			// nothing to do. fail immediately
			log.warning("Unable to upload data for request "+request.getRequestId()+"- no aggregator specified");
			return;
		}
		// request should be in state Submitting
		// start execution
		executor.execute(new Execution(request));
		
	}

	private class Execution implements Runnable, OutputWriter{
		private RetrievedRequest request;
		private DataSource result;
		Execution(RetrievedRequest request){
			this.request = request;
		}

		private void changeStatus(RequestStatus status, String description){
			try {
				request.changeStatus(null, status, description);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Unable to change status for request "+request.getRequestId()+" to "+status, e);
			}
		}
		@Override
		public void run() {
			
			try{
				this.result = request.getResultData();

				client.putRequestResult(request.getRequestId(), result.getContentType(), this);
				changeStatus(RequestStatus.Submitted, null);
			}catch( Throwable e ){
				// execution failed
				// TODO add stacktrace to error description
				log.log(Level.SEVERE, "Query execution failed for request "+request.getRequestId(),e);
				// XXX change broker status or aggregator status??
				changeStatus(RequestStatus.Failed, e.toString());
			}
		}

		@Override
		public void write(OutputStream dest) throws IOException {
			try( InputStream in = result.getInputStream() ){
				byte[] b = new byte[uploadBufferSize];
				int r = in.read(b);
				while( r != -1 ){
					dest.write(b, 0, r);
					r = in.read(b);
				}
			}
		}
	}


}
