package org.aktin.report.manager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.aktin.Preferences;
import org.aktin.dwh.DataExtractor;
import org.aktin.report.Report;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

public class ReportExecution {
	private static final Logger log = Logger.getLogger(ReportExecution.class.getName());	

	private Report report;
	private Instant fromTimestamp;
	private Instant endTimestamp;
	private Path reportDestination;
	private Map<String,?> prefs;
	private Path temp;

	/** files generated by R */
	private String[] files;
	/** Extracted data tables */
	private String[] dataFiles;
	/** Preference files */
	private String[] prefFiles;
	/** FOP input files and configuration */
	private String[] fopFiles;

	/**
	 * Don't delete anything. Mainly useful for debugging.
	 */
	private boolean keepIntermediateFiles;
	
	ReportExecution(Report report, Instant fromTimestamp, Instant endTimestamp, Path reportDestination){
		this.report = report;
		this.fromTimestamp = fromTimestamp;
		this.endTimestamp = endTimestamp;
		this.reportDestination = reportDestination;
	}

	public void setKeepIntermediateFiles(boolean keepFiles){
		this.keepIntermediateFiles = keepFiles;
	}

	/**
	 * Get preferences used to configure the report.
	 * @return preferences
	 */
	public Map<String,?> getPreferences(){
		return prefs;
	}

	/**
	 * Get the destination path which locates the generated report output.
	 * @return path
	 */
	public Path getTargetPath(){
		return reportDestination;
	}
	void createTempDirectory() throws IOException{
		temp = Files.createTempDirectory("report-"+report.getId());
		log.info("Using temporary directory: "+temp);
	}
	private Map<String,Object> selectPreferences(Preferences preferenceManager){
		// write report options and preferences to xml
		Map<String, Object> prefs = new HashMap<>();
		// TODO rename to report.data.start, report.data.end
		prefs.put("start", fromTimestamp.toString());
		prefs.put("end", endTimestamp.toString());
		prefs.put("report.data.timestamp", Instant.now().toString());
		prefs.put("report.template.id", this.report.getId());
		prefs.put("report.template.version", this.report.getVersion());
		// add requested local preferences, e.g. local.o/ou/cn
		for( String key : report.getRequiredPreferenceKeys() ){
			prefs.put(key, preferenceManager.get(key));
		}
		return prefs;
	}

	CompletableFuture<Void> extractData(DataExtractor extractor) throws IOException{
		// extract data and copy data files to temp directory
		return extractor.extractData(fromTimestamp, endTimestamp, report.getExportDescriptor(), temp)
				.thenAccept( a -> dataFiles = a );
	}

	void writePreferences(Preferences preferenceManager) throws IOException{
		this.prefs = selectPreferences(preferenceManager);
		prefFiles = writePreferences(temp);
	}

	/**
	 * Write preferences to properties and xml files. Creates
	 * two files named {@code prefs.properties} and {@code prefs.xml}
	 * @param dir output directory
	 * @return file names of generated files
	 * @throws IOException file error
	 */
	private String[] writePreferences(Path dir) throws IOException{
		String comments = "Generated preferences for report "+report.getId();
		Properties p = new Properties();
		final String[] files = {"prefs.properties","prefs.xml"};
//		p.putAll(this.prefs); (will fail for null values)
		// replace null values with empty string
		this.prefs.forEach( (k,v) -> p.put(k, (v==null)?"":v) );
		try( OutputStream out = Files.newOutputStream(dir.resolve(files[0]), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW) ){
			p.store(out, comments);			
		}
		try( OutputStream out = Files.newOutputStream(dir.resolve(files[1]), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW) ){
			p.storeToXML(out, comments);			
		}catch( IOException e ){
			// second file failed, make sure the first one is deleted
			if( !keepIntermediateFiles )try{
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

	void runR(Path rScriptExecutable) throws IOException{
		files = report.copyResourcesForR(temp);
		// run main script
		RScript rScript = new RScript(rScriptExecutable);
		rScript.runRscript(temp, files[0]);
		if( !keepIntermediateFiles ){
			// delete data files
			deleteFiles(temp, dataFiles);
			// delete copied R source files
			deleteFiles(temp, files);
		}
	}
	void runFOP() throws IOException{
		fopFiles = report.copyResourcesForFOP(temp);

		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer ft;
		try {		
			//Second file from Report interface is the XSL file	
			//ft = factory.newTransformer(new StreamSource(Files.newInputStream(workingPath.resolve(files[1])), files[1]));
			ft = factory.newTransformer(new StreamSource( temp.resolve(fopFiles[1]).toFile() ));
		} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			throw new IOException("Unable to construct FOP transformation",e);
		}

		FopFactory ff = FopFactory.newInstance(temp.toUri());
		FOUserAgent ua = ff.newFOUserAgent();
		FOEventListener events = new FOEventListener();
		ua.getEventBroadcaster().addEventListener(events);
		try( OutputStream out = Files.newOutputStream(reportDestination) ){
			// Step 3: Construct fop with desired output format
			Fop fop = ff.newFop(MimeConstants.MIME_PDF, ua, out);
			// configuration of transformer factory
			// First file from Report interface is the XML input (Source)
			Source src = new StreamSource(temp.resolve(fopFiles[0]).toFile());
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
		if( !events.isEmpty() ){
			log.warning("FOP errors: "+events.getSummary());
			//throw new IOException("Errors during FOP processing"); //do not throw until FOP-"unstable"-Errors are solved
		}
		if( !keepIntermediateFiles ){
			// preference files no longer needed
			deleteFiles(temp, prefFiles);
	
			// delete FOP input files
			deleteFiles(temp, fopFiles);
		}
	}

	private void reportAndRemoveRemainingFiles(Report report, Path dir, String[] leftFiles){
		StringBuilder sb = new StringBuilder();
		for( int i=0; i<leftFiles.length; i++ ){
			if( i != 0 ){
				sb.append(' ');					
			}
			sb.append(leftFiles[i]);

			if( keepIntermediateFiles ){
				continue;
			}
			try {
				Files.delete(dir.resolve(leftFiles[i]));
			} catch (IOException e) {
				log.warning("Unable to remove remaining file: "+leftFiles[i]);
			}
		}
		log.warning("Report "+report.getId()+" left files: "+sb.toString());
	}

	void cleanup() throws IOException{
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
		if( !keepIntermediateFiles ){
			Files.delete(temp);
		}
	}
}
