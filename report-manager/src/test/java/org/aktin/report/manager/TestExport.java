package org.aktin.report.manager;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;

import de.sekmi.histream.ObservationFactory;
import de.sekmi.histream.export.MemoryExportWriter;
import de.sekmi.histream.export.config.ExportDescriptor;
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

public class TestExport {
	// TODO validate CDA to export
	
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
		
		GroupedXMLReader reader = new GroupedXMLReader(of, getClass().getResourceAsStream("/demo-eav-data.xml"));
		ExportDescriptor ed = ExportDescriptor.parse(TestExport.class.getResourceAsStream("/export-descriptor.xml"));
		MemoryExportWriter ew = new MemoryExportWriter();
		ed.newExport().export(reader, ew);
		reader.close();
		ew.close();
		ew.dump();
		
	}
	
	// Dump data from a local i2b2 database to stdout (in eav-xml format)
	public static void main(String[] args) throws Exception{
		DataSource t = new LocalI2b2DataSource();
		Map<String,String> config = new HashMap<>();
		config.put("project", "AKTIN");
		PostgresPatientStore ps = new PostgresPatientStore(t,config);
		PostgresVisitStore vs = new PostgresVisitStore(t,config);
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

}
