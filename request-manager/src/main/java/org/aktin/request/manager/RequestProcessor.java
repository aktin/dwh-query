package org.aktin.request.manager;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.aktin.Preferences;
import org.aktin.broker.query.QueryHandlerFactory;
import org.aktin.broker.query.aggregate.rscript.RHandlerFactory;
import org.aktin.broker.query.sql.SQLHandlerFactory;
import org.aktin.broker.request.RetrievedRequest;
import org.aktin.dwh.PreferenceKey;
import org.w3c.dom.Element;

@Singleton
public class RequestProcessor implements Consumer<RetrievedRequest>{
//	private static final Logger log = Logger.getLogger(RequestProcessor.class.getName());

	private Executor executor;
	private QueryHandlerFactory[] handlerFactories;
	private ZoneId localZone;

	@Inject
	private Preferences prefs;

	public RequestProcessor(){
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

		handlerFactories = new QueryHandlerFactory[] {
				new SQLHandlerFactory(crc),
				new RHandlerFactory(Paths.get(prefs.get(PreferenceKey.rScriptBinary)))
		};
		
		localZone = ZoneId.of(prefs.get(PreferenceKey.timeZoneId));
	}

	@Override
	public void accept(RetrievedRequest request){
		// start execution
		executor.execute(new RequestExecution(this, request));
	}

	public QueryHandlerFactory findHandlerFactory(Element element) {
		for( QueryHandlerFactory fac : handlerFactories ){
			if( Objects.equals(fac.getNamespace(), element.getNamespaceURI())
					&& fac.getElementName().equals(element.getNodeName()) ){
				// first matching factory wins
				return fac;
			}
		}
		// no factory found
		return null;
	}

	/**
	 * Compile a list of properties for the specified handler factory
	 * @param request request to be executed
	 * @param factory factory used to execute the request
	 * @return properties
	 */
	public Map<String,String> compileProperties(RetrievedRequest request, QueryHandlerFactory factory){
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


}
