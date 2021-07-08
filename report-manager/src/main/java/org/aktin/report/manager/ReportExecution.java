package org.aktin.report.manager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.aktin.Preferences;
import org.aktin.dwh.DataExtractor;
import org.aktin.dwh.ExtractedData;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.GeneratedReport;
import org.aktin.report.Report;
import org.aktin.scripting.r.AbnormalTerminationException;
import org.aktin.scripting.r.RScript;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * Generation of reports separated into distinct execution steps,
 * which need to be performed in the following sequence:
 * <ol>
 *  <li>{@link #createTempDirectory(Path)}</li>
 *  <li>{@link #extractData(DataExtractor)}</li>
 *  <li>{@link #writePreferences(Preferences, Map)}</li>
 *  <li>{@link #runR(Path)}</li>
 *  <li>{@link #runFOP()}</li>
 *  <li>{@link #cleanup()}</li>
 * </ol>
 * See {@link ReportManagerImpl#generateReport(org.aktin.report.ReportInfo, Path)}
 * @author R.W.Majeed
 *
 */
class ReportExecution implements GeneratedReport, URIResolver{
	private static final Logger log = Logger.getLogger(ReportExecution.class.getName());	

	private Report report;
	private Instant fromTimestamp;
	private Instant endTimestamp;
	private Instant dataTimestamp;
	private Path reportDestination;
	private Map<String,String> prefs;
	private Path temp;

	/** files generated by R */
	private String[] files;
	/** Extracted data tables */
	private ExtractedData dataFiles;
	/** Preference files */
	private String[] prefFiles;
	/** FOP input files and configuration */
	private String[] fopFiles;

	/**
	 * Don't delete anything. Mainly useful for debugging.
	 */
	private boolean keepIntermediateFiles;
	private Set<Path> virtuallyDeleted;

	private SAXParserFactory parserFactory;

	ReportExecution(Report report, Instant fromTimestamp, Instant endTimestamp, Path reportDestination){
		this.report = report;
		this.fromTimestamp = fromTimestamp;
		this.endTimestamp = endTimestamp;
		this.reportDestination = reportDestination;
		parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(true);
		parserFactory.setValidating(false);
		try {
			parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch (SAXNotRecognizedException | SAXNotSupportedException | ParserConfigurationException e) {
			log.warning("Unable to set FEATURE_SECURE_PROCESSING in parser factory");
		}
	}

	public void setKeepIntermediateFiles(boolean keepFiles){
		this.keepIntermediateFiles = keepFiles;
		if( keepIntermediateFiles ){
			this.virtuallyDeleted = new HashSet<>();
		}
	}

	@Override
	public Map<String,String> getPreferences(){
		return prefs;
	}

	@Override
	public Path getLocation(){
		return reportDestination;
	}
	void createTempDirectory(Path base) throws IOException{
		// TODO use a different main temp directory from preferences, other from the system temp directory
		if( base == null ){
			temp = Files.createTempDirectory("report-"+report.getId());
			// use system temp dir
		}else{
			temp = Files.createTempDirectory(base, "report-"+report.getId());
		}
		log.info("Using temporary directory: "+temp);
	}

	protected static final String formatIsoTimestamp(Instant timestamp, ZoneId tz){
		return timestamp.atZone(tz).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
	private Map<String,String> selectPreferences(Preferences preferenceManager, Map<String,String> preset){
		// write report options and preferences to xml
		Map<String, String> prefs = new HashMap<>(preset);
		// write timestamps with local timezone
		ZoneId tz = ZoneId.of(preferenceManager.get(PreferenceKey.timeZoneId));
		prefs.put("report.data.start", formatIsoTimestamp(fromTimestamp,tz));
		prefs.put("report.data.end", formatIsoTimestamp(endTimestamp,tz));
		prefs.put("report.data.timestamp", formatIsoTimestamp(getDataTimestamp(),tz));
		// report.data.patients
		// report.data.encounters
		prefs.put("report.template.id", getTemplateId());
		prefs.put("report.template.version", getTemplateVersion());
		// add requested local preferences, e.g. local.o/ou/cn
		for( String key : report.getRequiredPreferenceKeys() ){
			prefs.put(key, preferenceManager.get(key));
		}
		// add info from extraction
		prefs.put("report.data.patients", Integer.toString(dataFiles.getPatientCount()));
		prefs.put("report.data.encounters", Integer.toString(dataFiles.getVisitCount()));
		return prefs;
	}

	CompletableFuture<Void> extractData(DataExtractor extractor) throws IOException{
		// set timestamp
		this.dataTimestamp = Instant.now();
		// extract data and copy data files to temp directory
		return extractor.extractData(fromTimestamp, endTimestamp, report.getExportDescriptor(), temp)
				.thenAccept( a -> dataFiles = a );
	}

	int getPatientCount(){
		if( dataFiles != null ){
			return dataFiles.getPatientCount();
		}else{
			return 0;
		}
	}

	void writePreferences(Preferences preferenceManager, Map<String,String> reportConfiguration) throws IOException{
		this.prefs = selectPreferences(preferenceManager, reportConfiguration);
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
			try{
				Files.delete(dir.resolve(files[0]));
			}catch( IOException e2 ){
				e.addSuppressed(e2);
			}
			throw e;
		}
		return files;
	}
	private void deleteFiles(Path dir, String... files) throws IOException{
		for( String file : files ){
			Path path = dir.resolve(file);
			if( !keepIntermediateFiles ){
				Files.delete(path);
			}else{
				// remember file as deleted
				virtuallyDeleted.add(path);
			}
		}
	}

	void runR(Path rScriptExecutable, int timeout, boolean debugging) throws IOException, TimeoutException, AbnormalTerminationException{
		files = report.copyResourcesForR(temp);
		// run main script
		RScript rScript = new RScript(rScriptExecutable);
		try {
			rScript.setDebugPrintMode(debugging);
			rScript.runRscript(temp, files[0], timeout);
		} finally {
			try {
				if (!debugging) {
					// delete data files
					deleteFiles(temp, dataFiles.getDataFileNames());
					// delete copied R source files
					deleteFiles(temp, files);
				}
			} catch (IOException e) {
				log.log(Level.WARNING, "Unable to delete generated files for R execution", e);
			}
		}
	}

	private XMLReader constructReader() throws IOException{
		XMLReader r;
		try {
			SAXParser parser = parserFactory.newSAXParser();
// do we need this?
//			try{
//				parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
//			}catch( SAXNotRecognizedException f ){
//				log.warning("SAXParser does not support ACCESS_EXTERNAL_DTD property");
//			}
			r = parser.getXMLReader();
		} catch (SAXException | ParserConfigurationException e) {
			throw new IOException("Unable to create SAX XML reader", e);
		}
		r.setEntityResolver(new NullEntityResolver());
		return r;
	}
	private Transformer createTransformer(Source source) throws IOException{
		TransformerFactory factory = TransformerFactory.newInstance();
		// configuration of transformer factory
		Transformer transformer;
		try {		
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
//			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
//			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
			if( source == null ){
				transformer = factory.newTransformer();
			}else{
				transformer = factory.newTransformer(source);
			}
			transformer.setErrorListener(new TransformationErrorListener(log));
		} catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
			throw new IOException("Unable to construct FOP transformation",e);
		}
		return transformer;
	}
	private static class NullEntityResolver implements EntityResolver {
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			log.finest("Entity resolved to empty resource: "+ publicId + " / " + systemId);
			return new InputSource(new ByteArrayInputStream(new byte[0]));
		}
	}
	private Source createSource(Path path) throws IOException{
		return  new SAXSource(constructReader(), new InputSource(path.toUri().toString()));
	}
	void runFOP() throws IOException{
		fopFiles = report.copyResourcesForFOP(temp);

		//Second file from Report interface is the XSL file	
		Transformer ft = createTransformer(createSource(temp.resolve(fopFiles[1])));
		ft.setURIResolver(this);
		FopFactory ff = FopFactory.newInstance(temp.toUri());
		FOUserAgent ua = ff.newFOUserAgent();
		FOEventListener events = new FOEventListener();
		ua.getEventBroadcaster().addEventListener(events);
		try( OutputStream out = Files.newOutputStream(reportDestination) ){
			// Step 3: Construct fop with desired output format
			Fop fop = ff.newFop(MimeConstants.MIME_PDF, ua, out);
			// configuration of transformer factory
			// First file from Report interface is the XML input (Source)
			Source src = createSource(temp.resolve(fopFiles[0]));
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
			log.warning("FOP errors ("+events.eventCount+"): "+events.getSummary());
			//throw new IOException("Errors during FOP processing"); //do not throw until FOP-"unstable"-Errors are solved
		}
		// preference files no longer needed
		deleteFiles(temp, prefFiles);

		// delete FOP input files
		deleteFiles(temp, fopFiles);

		delete_r_generated_files(temp);
	}

	private void delete_r_generated_files(Path dir) throws IOException{
		Path file = dir.resolve("r-generated-files.txt");
		if( Files.notExists(file) ){
			return;
		}
		try( BufferedReader r = Files.newBufferedReader(file, Charset.forName("UTF-8")) ){
			String line = r.readLine();
			List<String> lines = new ArrayList<>();
			while( line != null ){
				lines.add(line);
				line = r.readLine();
			}
			deleteFiles(dir, lines.toArray(new String[lines.size()]));
		}
		// delete the r-generated-files.txt itself
		Files.delete(file);
	}
	private void reportAndRemoveRemainingFiles(Report report, Path dir, String[] leftFiles){
		StringBuilder sb = new StringBuilder();
		for( int i=0; i<leftFiles.length; i++ ){
			Path path = dir.resolve(leftFiles[i]);

			if( keepIntermediateFiles ){
				if( !virtuallyDeleted.contains(path) ){
					sb.append(' ').append(leftFiles[i]);
				}
				continue;
			}else{
				sb.append(' ').append(leftFiles[i]);
			}
			try {
				Files.delete(path);
			} catch (IOException e) {
				log.warning("Unable to remove remaining file: "+leftFiles[i]);
			}
		}
		if( sb.length() > 0 ){
			log.warning("Report "+report.getId()+" left files:"+sb.toString());
		}
	}

	void cleanup() throws IOException{
		// output remaining files for debugging
		String[] leftFiles;
		try( Stream<Path> remaining = Files.list(temp) ){
			leftFiles = remaining.map(path -> temp.relativize(path).toString()).toArray(len -> new String[len]);
		}
		// report forgotten files
		reportAndRemoveRemainingFiles(report, temp, leftFiles);
		// remove directory
		if( !keepIntermediateFiles ){
			Files.delete(temp);
		}
	}

	@Override
	public String getMediaType() {
		return "application/pdf";
	}

	@Override
	public Instant getStartTimestamp() {
		return fromTimestamp;
	}

	@Override
	public Instant getEndTimestamp() {
		return endTimestamp;
	}

	@Override
	public Instant getDataTimestamp() {
		return dataTimestamp;
	}

	@Override
	public String getTemplateId() {
		return this.report.getId();
	}

	@Override
	public String getTemplateVersion() {
		 return this.report.getVersion();
	}

	@Override
	public Source resolve(String href, String base) throws TransformerException {
//		log.info("Resolving '"+href+"' / base="+base);
		try {
			return createSource(temp.resolve(href));
		} catch (IOException e) {
			throw new TransformerException("Unable to resolve source: "+href, e);
		}
	}

//	private Source createOfflineSource(Path path) throws IOException{
//	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//	factory.setValidating(false); // turns off validation
//	factory.setSchema(null);      // turns off use of schema
//	                          // but that's *still* not enough!
//	factory.setNamespaceAware(true);
//	DocumentBuilder builder;
//	try {
//		builder = factory.newDocumentBuilder();
//	} catch (ParserConfigurationException e) {
//		throw new IOException(e);
//	}
//	builder.setEntityResolver(new NullEntityResolver());
//	Document dom;
//	try( InputStream in = Files.newInputStream(path) ) {
//		dom = builder.parse(in);
//	} catch (SAXException e) {
//		throw new IOException(e);
//	}
//	return new DOMSource(dom, path.toUri().toString());
//}
//private Path createFoXml() throws IOException{
//	//Second file from Report interface is the XSL file	
//	Transformer ft = createTransformer(createOfflineSource(temp.resolve(fopFiles[1])));
//	ft.setURIResolver(this);
//	Path foXML = Files.createTempFile(temp, "fo", ".xml");
//	try( OutputStream out = Files.newOutputStream(foXML) ){
//		Source src = createOfflineSource(temp.resolve(fopFiles[0]));
//	    // Resulting SAX events (the generated FO) must be piped through to FOP
//	    Result res = new StreamResult(out);
//	    ft.transform(src, res);	
//	} catch (TransformerException e) {
//		throw new IOException("FOP transformation failed", e);
//	}
//	return foXML;
//}
//private void createFoPDF(Path foXML) throws IOException{
//	FopFactory ff = FopFactory.newInstance(temp.toUri());
//	FOUserAgent ua = ff.newFOUserAgent();
//	FOEventListener events = new FOEventListener();
//	ua.getEventBroadcaster().addEventListener(events);
//	Transformer ft = createTransformer(null);
//	try( OutputStream out = Files.newOutputStream(reportDestination) ){
//		Fop fop = ff.newFop(MimeConstants.MIME_PDF, ua, out);
//		ft.transform(new SAXSource(new InputSource(foXML.toUri().toString())), new SAXResult(fop.getDefaultHandler()));
//	} catch (FOPException e) {
//		throw new IOException(e);
//	} catch (TransformerException e) {
//		throw new IOException("FOP transformation failed",e);
//	}
//	if( !events.isEmpty() ){
//		log.warning("FOP errors: "+events.getSummary());
//		//throw new IOException("Errors during FOP processing"); //do not throw until FOP-"unstable"-Errors are solved
//	}
//}
//protected void runFOP_twostep() throws IOException{
//	fopFiles = report.copyResourcesForFOP(temp);
//	Path fo = createFoXml();
//	createFoPDF(fo);
//	// fo XML can be deleted
//	deleteFiles(temp, fo.relativize(temp).toString());
//	// preference files no longer needed
//	deleteFiles(temp, prefFiles);
//
//	// delete FOP input files
//	deleteFiles(temp, fopFiles);
//
//	delete_r_generated_files(temp);		
//}
}
