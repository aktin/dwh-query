package org.aktin.request.manager;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aktin.broker.query.QueryHandler;
import org.aktin.broker.query.QueryHandlerFactory;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;
import org.w3c.dom.Element;

public class RequestProcessor implements Consumer<RetrievedRequest>{
	private static final Logger log = Logger.getLogger(RequestProcessor.class.getName());

	private Executor executor;
	private List<QueryHandlerFactory> handlers;

	public RequestProcessor(){
		handlers = new ArrayList<>();
	}

	//@Resource(lookup="java:comp/DefaultManagedExecutorService")
	public void setExecutor(Executor executor){
		this.executor = executor;
	}

	@Override
	public void accept(RetrievedRequest request){
		// start execution
		executor.execute(new Execution(request));
	}

	private Map<String,String> compileProperties(RetrievedRequest request){
		Map<String, String> m = new HashMap<>();
		Instant a = request.getRequest().getScheduledTimestamp();
		// TODO calculate period
		//Period d = request.getRequest().getQuery().schedule.duration;
		// probably need local time
		// XXX for testing use a difference of 30 days
		Instant b = a.minusSeconds(60*60*24*30);
		// order a before b
		if( a.isAfter(b) ){
			// swap a and b
			Instant c = a;
			a = b;
			b = c;
		}
		// TODO use date format as specified by handler/processor
		m.put("data.start", a.toString());
		m.put("data.end", b.toString());
		m.put("data.timestamp", Instant.now().toString());
		// TODO add handler factory class name
		return m;
	}
	private class Execution implements Runnable{
		private RetrievedRequest request;
		private QueryHandlerFactory factory;
		private Element source;
		
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
			
			findHandlerFactory();
			if( factory == null ){
				// unable to execute query
				// fail immediately
				String msg = "No handler found for query extensions";
				log.severe(msg);
				changeStatus(RequestStatus.Failed, msg);
				// done
				return;
			}

			Map<String,String> props = compileProperties(request);
			
			
			try{
				request.setProcessing(props);
				// write properties and change status to processing
				QueryHandler h = factory.parse(source, props::get);
				Objects.requireNonNull(h, "Query handler parsing failed");
				request.createResultData(h.getResultMediaType());
				try( OutputStream out = request.getResultData().getOutputStream() ){
					h.execute(out);
				}
				changeStatus(RequestStatus.Completed, null);
			}catch( Throwable e ){
				// execution failed
				// TODO add stacktrace to error description
				log.log(Level.SEVERE, "Query execution failed for request "+request.getRequestId(),e);
				changeStatus(RequestStatus.Failed, e.toString());
			}
		}
		private void findHandlerFactory(){
			List<Element> ext = request.getRequest().getQuery().extensions;
			for( Element el : ext ){
				for( QueryHandlerFactory fac : handlers ){
					if( Objects.equals(fac.getNamespace(), el.getNamespaceURI())
							&& fac.getElementName().equals(el.getNodeName()) ){
						factory = fac;
						source = el;
						break;
					}
				}
				// first matching handler factory wins
				if( factory != null ){
					break;
				}
			}
		}
	}


}
