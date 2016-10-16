package org.aktin.report.manager;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.sql.DataSource;
import javax.xml.transform.Source;

import org.aktin.dwh.DataExtractor;

import de.sekmi.histream.ObservationFactory;
import de.sekmi.histream.export.TableExport;
import de.sekmi.histream.export.config.Concept;
import de.sekmi.histream.export.config.EavTable;
import de.sekmi.histream.export.config.ExportDescriptor;
import de.sekmi.histream.export.config.ExportException;
import de.sekmi.histream.export.csv.CSVWriter;
import de.sekmi.histream.i2b2.I2b2Extractor;
import de.sekmi.histream.i2b2.I2b2ExtractorFactory;
import de.sekmi.histream.impl.ObservationFactoryImpl;

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
	private ObservationFactory of;
	// TODO use resource/inject annotation to get a ManagedExecutorService
	private Executor executor;

	/**
	 * Opens database connections to i2b2 and prepares
	 * SQL statements for execution.
	 * 
	 * @throws IOException io error
	 * @throws SQLException sql error
	 */
	public DataExtractorImpl(DataSource crc_ds) throws IOException, SQLException{
		// use JNDI for database connection
		of = new ObservationFactoryImpl();
		// TODO need patient and encounter extension? if yes, add to factory
		extractor = new I2b2ExtractorFactory(crc_ds, of);
	}

	/**
	 * Collect concept notations and checks whether the wildcard notations
	 * are used. Also makes sure, no IRI is used (not supported yet).
	 * 
	 * @param concepts concept iterator
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
	/**
	 * Extracts data to a directory. After return, the directory
	 * will contain the following files:
	 * <ul>
	 * 	<li>patients.txt</li>
	 *  <li>encounters.txt</li>
	 *  <li>[table_name].txt for each additional table
	 * </ul>
	 * <p>
	 * In case of a checked exception, it is guaranteed that no files and 
	 * directories are created.
	 * <p>
	 * @param fromTimestamp start timestamp for the data to extract
	 * @param endTimestamp end timestamp for the data to extract
	 * @param report report for which the data will be extracted
	 * @return File names of the created files
	 * 
	 * @throws IOException error writing files
	 * @throws SQLException error while extracting data
	 * @throws ExportException error with export processing
	 * @throws UnsupportedOperationException concepts are specified via IRI, which is currently not supported
	 */
	@Override
	public CompletableFuture<String[]> extractData(Instant fromTimestamp, Instant endTimestamp, Source exportDescriptor, Path destinationDir){
		ExportDescriptor ed = ExportDescriptor.parse(exportDescriptor);
		TableExport fac = new TableExport(ed);

		// load concept notations
		Iterable<Concept> concepts = ed.allConcepts();
		List<String> notations = new ArrayList<>();
		boolean hasWildcards = collectNotations(concepts, notations);
		extractor.setFeature(I2b2ExtractorFactory.ALLOW_WILDCARD_CONCEPT_CODES, hasWildcards);

		// configure CSV writer
		CSVWriter csv = new CSVWriter(destinationDir, '\t', ".txt");
		csv.setPatientTableName("patients");
		csv.setVisitTableName("encounters");

		return CompletableFuture.supplyAsync(() -> {
			// perform the export operation
			try( I2b2Extractor ext = extractor.extract(Timestamp.from(fromTimestamp), Timestamp.from(endTimestamp), notations) ){
				fac.export(ext, csv);
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
			return files;
			
		}, this.executor);
	}

	/**
	 * Closes data base connection to the i2b2 database
	 */
	@PreDestroy
	@Override
	public void close() throws IOException {
		try {
			extractor.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}		
	}
}
