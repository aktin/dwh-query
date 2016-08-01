package org.aktin.report.manager;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import de.sekmi.histream.ObservationFactory;
import de.sekmi.histream.i2b2.I2b2Extractor;
import de.sekmi.histream.i2b2.I2b2ExtractorFactory;
import de.sekmi.histream.i2b2.PostgresPatientStore;
import de.sekmi.histream.i2b2.PostgresVisitStore;
import de.sekmi.histream.impl.ObservationFactoryImpl;
import de.sekmi.histream.io.GroupedXMLWriter;

public class TestReportManager {

	public static void main(String[] args) throws SQLException, XMLStreamException, ClassNotFoundException, IOException{
		Class.forName("org.postgresql.Driver");
		LocalI2b2DataSource ds = new LocalI2b2DataSource();
		Map<String,String> config = new HashMap<>();
		config.put("project", "AKTIN");
		PostgresPatientStore patients = new PostgresPatientStore(ds, config);
		PostgresVisitStore visits = new PostgresVisitStore(ds, config);
		ObservationFactory of = new ObservationFactoryImpl(patients, visits);
		GroupedXMLWriter w = new GroupedXMLWriter(System.out);
		try( I2b2ExtractorFactory extractor = new I2b2ExtractorFactory(ds, of);
				I2b2Extractor ext = extractor.extract(Timestamp.valueOf("2000-01-01 00:00:00"), Timestamp.valueOf("2020-01-01 00:00:00"), null) ){
			ext.stream().forEach(w);
		}
		w.close();
		patients.close();
		visits.close();
	}
}
