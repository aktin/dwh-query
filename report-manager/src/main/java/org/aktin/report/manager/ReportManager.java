package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.aktin.Module;
import org.aktin.Preference;
import org.aktin.Preferences;
import org.aktin.dwh.DataExtractor;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.Report;

/**
 * Manage all registered reports. Generate
 * reports.
 * <p>
 * Future feature: load report specifications
 * without java code via dynamic CDI producers
 * http://jdevelopment.nl/dynamic-cdi-producers/
 * CDI Bean interface and Extension which observes
 * {@link AfterBeanDiscovery}.
 * </p>
 * <p>
 * When generating a report, the report instance configuration
 * is written to a properties file as well as an xml file 
 * (e.g. start and end timestamps, local name, etc.).
 * To read these preferences from R, you can use
 * {@code read.table(file="Temp\\prefs.properties",sep="=",col.names=c("key","value")}
 * </p>
 *
 * @author R.W.Majeed
 *
 */
@Singleton
//@Preferences(group="reports")
public class ReportManager extends Module{
//	private static final Logger log = Logger.getLogger(ReportManager.class.getName());	
	@Inject @Any
	Instance<Report> cdiReports;
	private Report[] staticReports;
	
	@Inject @Preference(key=PreferenceKey.rScriptBinary)
	String rScript;
	
	private Executor executor;
	private boolean keepIntermediateFiles;
	

	/**
	 * Will be injected via {@link #setDataExtractor(DataExtractor)}
	 */
	private DataExtractor extractor;
	
	/**
	 * Will be injected via {@link #setPreferenceManager(Preferences)}
	 */
	private Preferences preferenceManager;
	
	/**
	 * Empty constructor for CDI
	 */
	protected ReportManager(){
	}

	/**
	 * Manually construction of report manager. Don't forget to call {@link #setDataExtractor(DataExtractor)}
	 * and {@link #setPreferenceManager(Preferences)}
	 * @param rScript
	 * @param reports
	 */
	public ReportManager(String rScript, Report...reports){
		this.rScript = rScript;
		this.staticReports = reports;
		this.keepIntermediateFiles = false;
	}

	@Inject
	public void setDataExtractor(DataExtractor extractor){
		this.extractor = extractor;
	}
	@Inject
	public void setPreferenceManager(Preferences prefs){
		this.preferenceManager = prefs;
	}

	@Resource
	public void setExecutor(Executor executor){
		this.executor = executor;
	}
	public Iterable<Report> reports(){
		if( cdiReports != null ) {
			return cdiReports;
		}else if( staticReports != null ){
			return Arrays.asList(staticReports);
		}else{
			return Collections.emptyList();
		}
	}

	public void setKeepIntermediateFiles(boolean keepFiles){
		this.keepIntermediateFiles = keepFiles;
	}
	public CompletableFuture<ReportExecution> generateReport(Report report, Instant fromTimestamp, Instant endTimestamp, Path reportDestination) throws IOException{
		// TODO find a way to pass report specific configuration. e.g. Map<String,Object>
		ReportExecution re = new ReportExecution(report, fromTimestamp, endTimestamp, reportDestination);
		re.createTempDirectory();

		// to keep all generated files, use #setKeepIntermediateFiles
		re.setKeepIntermediateFiles(keepIntermediateFiles);

		return re.extractData(extractor).thenApplyAsync( Void ->  {
			try {
				re.writePreferences(preferenceManager);
				re.runR(Paths.get(ReportManager.this.rScript));
				re.runFOP();
				re.cleanup();
			} catch (IOException e) {
				throw new CompletionException(e);
			}
			return re;
		}, getExecutor() );
	}

	private Executor getExecutor(){
		return executor;
	}
	/**
	 * Generate a report
	 * @param report report template
	 * @param fromTimestamp minimum timestamp for the report data
	 * @param endTimestamp maximum timestamp for the report data
	 * @param reportDestination destination file where the report file will be written to
	 * @throws IOException error
	 */
	@Deprecated
	public void generateReportNow(Report report, Instant fromTimestamp, Instant endTimestamp, Path reportDestination) throws IOException{
		ReportExecution re = new ReportExecution(report, fromTimestamp, endTimestamp, reportDestination);
		re.createTempDirectory();

		try {
			re.extractData(extractor).get();
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (ExecutionException e) {
			throw new IOException(e.getCause());
		}

		re.writePreferences(preferenceManager);

		re.runR(Paths.get(this.rScript));
		re.runFOP();
		// 
		
		re.cleanup();
	}

}
