package org.aktin.report.manager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
	@Inject @Any
	Instance<Report> cdiReports;
	private Report[] staticReports;
	
	@Inject @Preference(key=PreferenceKey.i2b2Project)
	String rScript;
	

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
		
	}

	@Inject
	public void setDataExtractor(DataExtractor extractor){
		this.extractor = extractor;
	}
	@Inject
	public void setPreferenceManager(Preferences prefs){
		this.preferenceManager = prefs;
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

	/**
	 * Write preferences to properties and xml files. Creates
	 * two files named {@code prefs.properties} and {@code prefs.xml}
	 * @param prefs preferences
	 * @param dir output directory
	 * @param comments comments
	 * @return file names of generated files
	 * @throws IOException file error
	 */
	private String[] writePreferences(Map<String, ?> prefs, Path dir, String comments) throws IOException{
		Properties p = new Properties();
		final String[] files = {"prefs.properties","prefs.xml"};
		p.putAll(prefs);
		try( OutputStream out = Files.newOutputStream(dir.resolve(files[0]), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW) ){
			p.store(out, comments);			
		}
		try( OutputStream out = Files.newOutputStream(dir.resolve(files[1]), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW) ){
			p.storeToXML(out, comments);			
		}catch( IOException e ){
			// second file failed, make sure the first one is deleted
			try{
				Files.delete(dir.resolve(files[0]));
			}catch( IOException e2 ){
				e.addSuppressed(e2);
			}
			throw e;
		}
		return files;
	}
	private void deleteFiles(Path dir, String[] files) throws IOException{
		for( String file : files ){
			Files.delete(dir.resolve(file));
		}		
	}

	/**
	 * Generate a report
	 * @param report report template
	 * @param fromTimestamp minimum timestamp for the report data
	 * @param endTimestamp maximum timestamp for the report data
	 * @param reportDestination destination file where the report file will be written to
	 * @throws IOException error
	 */
	public void generateReport(Report report, Instant fromTimestamp, Instant endTimestamp, Path reportDestination) throws IOException{
		Path temp = Files.createTempDirectory("report-"+report.getId());
		String[] files;
		String[] dataFiles;
		String[] prefFiles;
		
		// extract data and copy data files to temp directory
		try {
			dataFiles = extractor.extractData(fromTimestamp, endTimestamp, report.getExportDescriptor(), temp);
		} catch (SQLException e) {
			throw new IOException(e);
		}

		// write report options and preferences to xml
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("start", fromTimestamp.toString());
		prefs.put("end", endTimestamp.toString());
		// add requested local preferences, e.g. local.o/ou/cn
		for( String key : report.getRequiredPreferenceKeys() ){
			prefs.put(key, preferenceManager.get(key));
		}
		prefFiles = writePreferences(prefs, temp, "Generated preferences for report "+report.getId());
		
		synchronized( report ){
			files = report.copyResourcesForR(temp);
			// run main script
			RScript rScript = new RScript(Paths.get(this.rScript));
			rScript.runRscript(temp, files[0]);
			// delete data files
			deleteFiles(temp, dataFiles);
			// delete copied R source files
			deleteFiles(temp, files);
			// run Apache FOP
			
		}
		deleteFiles(temp, prefFiles);

		// remove directory
		Files.delete(temp);
	}
	
}
