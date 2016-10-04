package org.aktin.report.manager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.aktin.Module;
import org.aktin.Preference;
import org.aktin.Preferences;
import org.aktin.dwh.DataExtractor;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.Report;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

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
	private static final Logger log = Logger.getLogger(ReportManager.class.getName());	
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
		
		String[] fopFiles;

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
			
			fopFiles = report.copyResourcesForFOP(temp);
			runFOP(fopFiles, temp, reportDestination);
		}
		// 
		
		deleteFiles(temp, prefFiles);

		deleteFiles(temp, fopFiles);
		// output remaining files for debugging
		String[] leftFiles;
		try( Stream<Path> remaining = Files.list(temp) ){
			leftFiles = remaining.map(path -> temp.relativize(path).toString()).toArray(len -> new String[len]);
		}
		if( leftFiles.length != 0 ){
			// report forgotten files
			reportAndRemoveRemainingFiles(report, temp, leftFiles);
		}
		// remove directory
		Files.delete(temp);
	}

	private void runFOP(String[] files, Path workingPath, Path destPDF) throws IOException{
		Transformer ft;
		try {
			//Second file from Report interface is the XSL file
			ft = TransformerFactory.newInstance().newTransformer(new StreamSource(Files.newInputStream(workingPath.resolve(files[1])), files[1]));
		} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			throw new IOException("Unable to construct FOP transformation",e);
		}

		FopFactory ff = FopFactory.newInstance(workingPath.toUri());
		try( OutputStream out = Files.newOutputStream(destPDF) ){
			// Step 3: Construct fop with desired output format
			Fop fop = ff.newFop(MimeConstants.MIME_PDF, out);
			TransformerFactory factory = TransformerFactory.newInstance();
			// configuration of transformer factory
			//First file from Report interface is the XML input (Source)
			Source src = new StreamSource(workingPath.resolve(files[0]).toFile());
		    // Resulting SAX events (the generated FO) must be piped through to FOP
		    Result res = new SAXResult(fop.getDefaultHandler());
		    // Step 6: Start XSLT transformation and FOP processing
		    ft.transform(src, res);
		    // XXX fatal FOP errors will not stop the transformation
		    // TODO add FOP error handling
		} catch (FOPException e) {
			throw new IOException(e);
		} catch (TransformerException e) {
			throw new IOException("FOP transformation failed",e);
		}
	}
	private void reportAndRemoveRemainingFiles(Report report, Path dir, String[] leftFiles){
		StringBuilder sb = new StringBuilder();
		for( int i=0; i<leftFiles.length; i++ ){
			if( i != 0 ){
				sb.append(' ');					
			}
			sb.append(leftFiles[i]);
			try {
				Files.delete(dir.resolve(leftFiles[i]));
			} catch (IOException e) {
				log.warning("Unable to remove remaining file: "+leftFiles[i]);
			}
		}
		log.warning("Report "+report.getId()+" left files: "+sb.toString());		
	}


}
