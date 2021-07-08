package org.aktin.report.manager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import javax.annotation.Resource;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.aktin.Module;
import org.aktin.Preferences;
import org.aktin.dwh.DataExtractor;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.GeneratedReport;
import org.aktin.report.InsufficientDataException;
import org.aktin.report.Report;
import org.aktin.report.ReportInfo;
import org.aktin.report.ReportManager;
import org.aktin.scripting.r.AbnormalTerminationException;

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
public class ReportManagerImpl extends Module implements ReportManager{
//	private static final Logger log = Logger.getLogger(ReportManager.class.getName());	
	@Inject @Any
	Instance<Report> cdiReports;
	private Report[] staticReports;
	
//	@Inject @Preference(key=PreferenceKey.rScriptBinary)
	String rScript;
	
	private Executor executor;
	private boolean keepIntermediateFiles;

	private Path tempDir;

	/*
	 * Timeout in ms for single script execution
	 */
	private Integer timeout;

	/*
	 * Boolean if Rscript shall run in Debugging Mode.
	 * Will print additional startup information and full error log and will keep all generated files if set on true
	 */
	private Boolean isDebugMode;

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
	protected ReportManagerImpl(){
	}

	/**
	 * Manually construction of report manager.
	 * <p>
	 * This will override the rScript and tempDirectory preferences, which would be otherwise loaded from
	 * the preference manager (see {@link #setPreferenceManager(Preferences)}).
	 * </p>
	 * <p>
	 * You still need to call {@link #setDataExtractor(DataExtractor)}, {@link #setPreferenceManager(Preferences)}
	 * and {@link #setExecutor(Executor)} in order to generate reports.
	 * </p>
	 * and {@link #setPreferenceManager(Preferences)}
	 * @param rScript Rscript executable file
	 * @param tempDir temporary directory
	 * @param reports reports to use
	 */
	public ReportManagerImpl(String rScript, Path tempDir, Report...reports){
		this.rScript = rScript;
		this.tempDir = tempDir;
		verifyTempDir();
		this.staticReports = reports;
		this.keepIntermediateFiles = false;
	}

	private void verifyTempDir(){
		// make sure the temp dir exists, create if necessary
		try {
			Files.createDirectories(tempDir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	@Inject
	public void setDataExtractor(DataExtractor extractor){
		this.extractor = extractor;
	}
	@Inject
	public void setPreferenceManager(Preferences prefs){
		this.preferenceManager = prefs;
		// load rScript and tempDir from preferences, if not overridden by manual constructor
		if( rScript == null ){
			rScript = prefs.get(PreferenceKey.rScriptBinary);
		}
		if( timeout == null){
			timeout = Integer.parseInt(preferenceManager.get(PreferenceKey.rScriptTimeout));
		}
		if( isDebugMode == null){
			isDebugMode = Boolean.parseBoolean(preferenceManager.get(PreferenceKey.rScriptDebug));
		}
		if( this.tempDir == null ){
			this.tempDir = Paths.get(prefs.get(PreferenceKey.reportTempPath));
			verifyTempDir();
		}
		String keepTemp = prefs.get(PreferenceKey.reportDebugKeepTempFiles);
		if( keepTemp != null ) {
			setKeepIntermediateFiles(Boolean.parseBoolean(keepTemp));
		}
	}

	@Resource(lookup="java:comp/DefaultManagedExecutorService")
	public void setExecutor(Executor executor){
		this.executor = executor;
	}
	/* (non-Javadoc)
	 * @see org.aktin.report.manager.ReportManager#reports()
	 */
	@Override
	public Iterable<Report> reports(){
		if( cdiReports != null ) {
			return cdiReports;
		}else if( staticReports != null ){
			return Arrays.asList(staticReports);
		}else{
			return Collections.emptyList();
		}
	}

	/* (non-Javadoc)
	 * @see org.aktin.report.manager.ReportManager#getReport(java.lang.String)
	 */
	@Override
	public Report getReport(String id){
		// TODO hashtable implementation for O(1) access
		for( Report report : reports() ){
			if( id.equals(report.getId()) ){
				return report;
			}
		}
		// not found
		return null;
	}

	public void setKeepIntermediateFiles(boolean keepFiles){
		this.keepIntermediateFiles = keepFiles;
	}


	@Override
	public CompletableFuture<? extends GeneratedReport> generateReport(ReportInfo reportInfo, Path reportDestination) throws IOException {
		Report report = getReport(reportInfo.getTemplateId());
		if( report == null ){
			throw new IllegalArgumentException("Report template not found: "+reportInfo.getTemplateId());
		}

		if( reportDestination == null ){
			// make sure tempDir exists
			Files.createDirectories(tempDir);
			// TODO infer file extension from media type. read media type from report template
			reportDestination = Files.createTempFile(tempDir, "report", ".pdf");
		}

		Objects.requireNonNull(extractor, "DataExtractor not defined");
		Objects.requireNonNull(getExecutor(), "Executor not defined");
		Objects.requireNonNull(preferenceManager, "PreferenceManager not defined");
		// TODO find a way to pass report specific configuration. e.g. Map<String,Object>
		ReportExecution re = new ReportExecution(report, reportInfo.getStartTimestamp(), reportInfo.getEndTimestamp(), reportDestination);
		re.createTempDirectory(tempDir);

		// to keep all generated files, use #setKeepIntermediateFiles
		re.setKeepIntermediateFiles(keepIntermediateFiles);

		return re.extractData(extractor).thenApplyAsync( Void ->  {
			try {
				if( re.getPatientCount() == 0 ){
					// fail early if there is no data to process
					throw new InsufficientDataException();
				}
				re.writePreferences(preferenceManager, reportInfo.getPreferences());
				re.runR(Paths.get(ReportManagerImpl.this.rScript), timeout, isDebugMode);
				re.runFOP();
				re.cleanup();
			} catch (IOException | TimeoutException | AbnormalTerminationException e) {
				throw new CompletionException(e);
			}
			return re;
		}, getExecutor() );
	}

	private Executor getExecutor(){
		return executor;
	}
	
}
