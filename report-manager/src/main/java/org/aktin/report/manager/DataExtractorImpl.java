package org.aktin.report.manager;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.transform.Source;

import org.aktin.Preferences;
import org.aktin.dwh.DataExtractor;
import org.aktin.dwh.ExtractedData;
import org.aktin.dwh.PreferenceKey;

import de.sekmi.histream.ObservationFactory;
import de.sekmi.histream.export.ExportSummary;
import de.sekmi.histream.export.TableExport;
import de.sekmi.histream.export.config.Concept;
import de.sekmi.histream.export.config.EavTable;
import de.sekmi.histream.export.config.ExportDescriptor;
import de.sekmi.histream.export.config.ExportException;
import de.sekmi.histream.export.csv.CSVWriter;
import de.sekmi.histream.i2b2.I2b2Extractor;
import de.sekmi.histream.i2b2.I2b2ExtractorFactory;
import de.sekmi.histream.i2b2.PostgresPatientStore;
import de.sekmi.histream.i2b2.PostgresVisitStore;

/**
 * Takes a time interval and concept list and extracts all
 * relevant data to a tabular representation which is readable
 * by R. 
 * <p> 
 * As file format, tab-separated-values are used. The first
 * line contains column headers. In the data values, all
 * newline characters and tab characters are replaced with spaces.
 * <p>
 * The patient and encounter tables are written always. For
 * each repeating concept, additional tables are written (e.g. diagnoses) 
 * 
 * @author R.W.Majeed
 *
 */
@Singleton
class DataExtractorImpl implements DataExtractor, Closeable{
	//private static final Logger log = Logger.getLogger(DataExtractor.class.getName());
	private I2b2ExtractorFactory extractor;
	private Executor executor; // will be filled via resource injection or setter method


	DataExtractorImpl(DataSource crc_ds, PostgresPatientStore patientStore, PostgresVisitStore visitStore, ObservationFactory factory) throws IOException, SQLException{
		extractor = new I2b2ExtractorFactory(crc_ds, factory);
		extractor.setPatientLookup(patientStore::lookupPatientNum);
		extractor.setVisitLookup(visitStore::lookupEncounterNum);
	}

	@Inject
	public DataExtractorImpl(Preferences prefs, ObservationFactory factory, PostgresPatientStore patientStore, PostgresVisitStore visitStore){
		
		try {
			InitialContext ctx = new InitialContext();
			DataSource crcDS = (DataSource)ctx.lookup(prefs.get(PreferenceKey.i2b2DatasourceCRC));
			extractor = new I2b2ExtractorFactory(crcDS, factory);
		} catch (SQLException | NamingException e) {
			throw new IllegalStateException("Unable to load extractor factory", e);
		}
		extractor.setPatientLookup(patientStore::lookupPatientNum);
		extractor.setVisitLookup(visitStore::lookupEncounterNum);		
	}

	@Resource
	public void setExecutor(ManagedExecutorService executor){
		this.executor = executor;
	}
	public void setExecutor(Executor executor){
		this.executor = executor;
	}

	/**
	 * Collect concept notations and checks whether the wildcard notations
	 * are used. Also makes sure, no IRI is used (not supported yet).
	 * 
	 * @param concepts iterator to read concepts from
	 * @param notations collection to store the notations. Use a {@link Set} to ensure distinct notations.
	 * @throws UnsupportedOperationException if concepts are specified via IRI, which is not supported currently
	 */
	private boolean collectNotations(Iterable<Concept> concepts, Collection<String> notations) throws UnsupportedOperationException{
		boolean hasWildcard = false;
		for( Concept concept : concepts ){
			if( concept.getIRI() != null ){
				throw new UnsupportedOperationException("Concept specification via IRI not supported yet: "+concept.getIRI());
			}else if( concept.getWildcardNotation() != null ){
				hasWildcard = true;
				notations.add(concept.getWildcardNotation());
			}else{
				notations.add(concept.getNotation());
			}
		}
		return hasWildcard;
	}

	@Override
	public CompletableFuture<ExtractedData> extractData(Instant fromTimestamp, Instant endTimestamp, Source exportDescriptor, Path destinationDir){
		Objects.requireNonNull(this.executor, "Executor not provided/injected");
		ExportDescriptor ed = ExportDescriptor.parse(exportDescriptor);
		TableExport fac = new TableExport(ed);

		// load concept notations
		Iterable<Concept> concepts = ed.allConcepts();
		// iterable may contain duplicates
		Set<String> notations = new HashSet<>(); // use set to guarantee distinct concepts
		boolean hasWildcards = collectNotations(concepts, notations);
		extractor.setFeature(I2b2ExtractorFactory.ALLOW_WILDCARD_CONCEPT_CODES, hasWildcards);

		// configure CSV writer
		CSVWriter csv = new CSVWriter(destinationDir, '\t', ".txt");
		csv.setPatientTableName("patients");
		csv.setVisitTableName("encounters");

		return CompletableFuture.supplyAsync(() -> {
			ExtractedDataImpl edi;
			// perform the export operation
			try( I2b2Extractor ext = extractor.extract(Timestamp.from(fromTimestamp), Timestamp.from(endTimestamp), notations) ){
				ExportSummary sum = fac.export(ext, csv);
				edi = new ExtractedDataImpl(sum);
			} catch (ExportException | SQLException | IOException e) {
				// wrap for completable
				throw new CompletionException(e);
			}
			csv.close(); // not needed

			// construct generated file names
			EavTable[] eav = ed.getEAVTables();
			String[] files = new String[eav.length+2];
			files[0] = csv.fileNameForTable("patients");
			files[1] = csv.fileNameForTable("encounters");
			for( int i=0; i<eav.length; i++ ){
				files[2+i] = csv.fileNameForTable(eav[i].getId());
			}
			edi.setDataFileNames(files);
			return edi;
			
		}, this.executor);
	}

	/**
	 * Closes data base connection to the i2b2 database
	 */
	@PreDestroy
	@Override
	public void close() {
		extractor.close();	
	}
}
