package org.aktin.request.manager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.aktin.Preferences;
import org.aktin.broker.query.QueryHandler;
import org.aktin.broker.query.QueryHandlerFactory;
import org.aktin.broker.query.aggregate.rscript.RHandlerFactory;
import org.aktin.broker.query.sql.SQLHandlerFactory;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;
import org.aktin.dwh.PreferenceKey;
import org.w3c.dom.Element;

@Singleton
public class RequestProcessor implements Consumer<RetrievedRequest>{
	private static final Logger log = Logger.getLogger(RequestProcessor.class.getName());

	private Executor executor;
	private List<QueryHandlerFactory> handlers;
	private ZoneId localZone;

	@Inject
	private Preferences prefs;

	public RequestProcessor(){
		handlers = new ArrayList<>();
	}

	@Resource(lookup="java:comp/DefaultManagedExecutorService")
	public void setExecutor(Executor executor){
		this.executor = executor;
	}

	@PostConstruct
	public void loadQueryHandlers() {
		// lookup data source
		String lookup = prefs.get(PreferenceKey.i2b2DatasourceCRC);
		DataSource crc;
		try {
			crc = (DataSource)(new InitialContext().lookup(lookup));
		} catch (NamingException e) {
			throw new RuntimeException("Unable to lookup CRC data source", e);
		}

		handlers.add(new SQLHandlerFactory(crc));
		handlers.add(new RHandlerFactory(Paths.get(prefs.get(PreferenceKey.rScriptBinary))));
		localZone = ZoneId.of(prefs.get(PreferenceKey.timeZoneId));
	}

	@Override
	public void accept(RetrievedRequest request){
		// start execution
		executor.execute(new Execution(request));
	}

	private Map<String,String> compileProperties(RetrievedRequest request, QueryHandlerFactory factory){
		Map<String, String> m = new HashMap<>();
		Instant a = request.getRequest().getReferenceTimestamp();
		// calculate period
		Period d = request.getRequest().getQuery().schedule.duration;
		// time periods larger than week and day need time zone information
		Instant b = a.atZone(localZone).plus(d).toInstant();
		
		// order a before b
		if( a.isAfter(b) ){
			// swap a and b
			Instant c = a;
			a = b;
			b = c;
		}
		// use date format as specified by handler/processor
		m.put("data.start", factory.formatTimestamp(a));
		m.put("data.end", factory.formatTimestamp(b));
		m.put("data.timestamp", factory.formatTimestamp(Instant.now()));
		// add handler factory class name
		m.put("query.handler", factory.getClass().getName());
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
			
			// TODO multiple handlers
			// TODO verify inbetween handlers that the remaining files in the directory are exactly the advertised results

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

			Map<String,String> props = compileProperties(request, factory);
			
			
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
				// add stacktrace to error description
				log.log(Level.SEVERE, "Query execution failed for request "+request.getRequestId(), e);
				changeStatus(RequestStatus.Failed, Util.stringStackTrace(e));
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
