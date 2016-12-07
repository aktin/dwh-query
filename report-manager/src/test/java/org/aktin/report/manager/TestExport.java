package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;

import org.aktin.dwh.DataExtractor;
import org.aktin.dwh.ExtractedData;
import org.junit.Test;

import de.sekmi.histream.ObservationFactory;
import de.sekmi.histream.export.ExportSummary;
import de.sekmi.histream.export.MemoryExportWriter;
import de.sekmi.histream.export.config.EavTable;
import de.sekmi.histream.export.config.ExportDescriptor;
import de.sekmi.histream.export.config.ExportException;
import de.sekmi.histream.export.csv.CSVWriter;
import de.sekmi.histream.i2b2.I2b2Extractor;
import de.sekmi.histream.i2b2.I2b2ExtractorFactory;
import de.sekmi.histream.i2b2.PostgresPatientStore;
import de.sekmi.histream.i2b2.PostgresVisitStore;
import de.sekmi.histream.impl.ObservationFactoryImpl;
import de.sekmi.histream.impl.SimplePatientExtension;
import de.sekmi.histream.impl.SimpleVisitExtension;
import de.sekmi.histream.io.GroupedXMLReader;
import de.sekmi.histream.io.GroupedXMLWriter;
import de.sekmi.histream.io.Streams;

public class TestExport implements DataExtractor{
	// TODO validate CDA to export
	
	private String resourceData;

	/**
	 * Use a small data set. Same as {@link #small()}.
	 */
	public TestExport(){
		resourceData = "/demo-eav-data.xml";
	}

	/**
	 * Use a large test data set
	 * @return extractor
	 */
	public static TestExport large(){
		TestExport e = new TestExport();
		e.resourceData = "/demo-eav-data.xml";
		return e;
	}
	/**
	 * Use a small test data set
	 * @return extractor
	 */
	public static TestExport small(){
		TestExport e = new TestExport();
		return e;
	}
	/**
	 * Read demo-eav-data.xml from src/test/resources and export all data to in-memory tables.
	 * The export descriptor is also located in src/test/resources.
	 * VT: bitte export descriptor vervollständigen
	 * 
	 * Exported data tables are dumped to stdout (for debugging export descriptor)
	 * @throws Exception test failure
	 */
	@Test
	public void verifyEavToExport() throws Exception{
		ObservationFactory of = new ObservationFactoryImpl(new SimplePatientExtension(), new SimpleVisitExtension());
		
		GroupedXMLReader reader = new GroupedXMLReader(of, getClass().getResourceAsStream(resourceData));
		ExportDescriptor ed = ExportDescriptor.parse(TestExport.class.getResourceAsStream("/export-descriptor.xml"));
		MemoryExportWriter ew = new MemoryExportWriter();
		ExportSummary summary = ed.newExport().export(reader, ew);
		
		reader.close();
		ew.close();
		//ew.dump();
		
	}
	
	// Dump data from a local i2b2 database to stdout (in eav-xml format)
	public static void main(String[] args) throws Exception{
		DataSource t = new LocalI2b2DataSource();
		String projectId = "AKTIN";
		PostgresPatientStore ps = new PostgresPatientStore();
		ps.open(t.getConnection(), projectId);
		PostgresVisitStore vs = new PostgresVisitStore();
		ps.open(t.getConnection(), projectId);
		ObservationFactory of = new ObservationFactoryImpl(ps, vs);
//		ExportDescriptor ed = JAXB.unmarshal(TestExport.class.getResourceAsStream("/export-descriptor.xml"), ExportDescriptor.class);
//		ExportDescriptor ed = ExportDescriptor.parse(TestExport.class.getResourceAsStream("/export-descriptor.xml"));
		try( I2b2ExtractorFactory ef = new I2b2ExtractorFactory(t, of) ){
			ef.setPatientLookup(ps::lookupPatientNum);
			ef.setVisitLookup(vs::lookupEncounterNum);
			try( I2b2Extractor e = ef.extract(Timestamp.valueOf("2010-01-01 00:00:00"), Timestamp.valueOf("2020-01-17 00:00:00"), null) ){
				GroupedXMLWriter w = new GroupedXMLWriter(System.out);
				//MemoryExportWriter w = new MemoryExportWriter();
				//ed.newExport().export(e, w);
				
				Streams.transfer(e, w);
				w.close();
			}
			
		}
		vs.close();
		ps.close();
		
		
	}

	@Override
	public CompletableFuture<ExtractedData> extractData(Instant fromTimestamp, Instant endTimestamp, Source exportDescriptor,
			Path destinationDir) {
		return CompletableFuture.supplyAsync( () -> {
			
		ObservationFactory of = new ObservationFactoryImpl(new SimplePatientExtension(), new SimpleVisitExtension());
		// TODO filter eav data with timestamps
		GroupedXMLReader reader;
		try {
			reader = new GroupedXMLReader(of, getClass().getResourceAsStream(resourceData));
		} catch (JAXBException | XMLStreamException | FactoryConfigurationError e) {
			throw new CompletionException(e);
		}
		ExportDescriptor ed = ExportDescriptor.parse(exportDescriptor);
		CSVWriter ew = new CSVWriter(destinationDir, '\t', ".txt");
		ew.setVisitTableName("encounters");
		ExtractedDataImpl edi;
		try {
			ExportSummary summary = ed.newExport().export(reader, ew);
			edi = new ExtractedDataImpl(summary);
			reader.close();
		} catch (ExportException | XMLStreamException | IOException e) {
			throw new CompletionException(e);
		}
		ew.close();

		// get produced file names
		EavTable[] t = ed.getEAVTables();
		String[] files = new String[2+t.length];
		files[0] = ew.fileNameForTable(ew.getPatientTableName());
		files[1] = ew.fileNameForTable(ew.getVisitTableName());
		for( int i=0; i<t.length; i++ ){
			files[2+i] = ew.fileNameForTable(t[i].getId());
		}
		edi.setDataFileNames(files);
		return edi;
		} );
	}

}
