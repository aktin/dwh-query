package org.aktin.report.manager;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import de.sekmi.histream.ObservationFactory;
import de.sekmi.histream.i2b2.I2b2Extractor;
import de.sekmi.histream.i2b2.I2b2ExtractorFactory;
import de.sekmi.histream.i2b2.PostgresPatientStore;
import de.sekmi.histream.i2b2.PostgresVisitStore;
import de.sekmi.histream.impl.ObservationFactoryImpl;
import de.sekmi.histream.io.GroupedXMLWriter;

import org.aktin.report.test.SimpleReport;
import org.junit.Assert;

public class TestReportManager {

	@Test
	public void verifyUnwrappedUncheckedException() throws InterruptedException{
		Runnable r1 = () -> {
			try{
				throw new IOException("test");
			}catch( Exception e ){
				throw new CompletionException(e);				
			}
		};
//		Runnable r2 = () -> {};
		
		Void v;
		try {
			v = CompletableFuture.runAsync(r1).get();
			Assert.assertNull(v);
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause().getClass() == IOException.class);
		}
	}

	@Test
	public void expectSimpleReportAvailable(){
		ReportManagerImpl manager = new ReportManagerImpl("asf", TestReportGeneration.getReportTempDir(), new SimpleReport());
		// check by id
		Assert.assertNotNull(manager.getReport(SimpleReport.class.getName()));
		// check in list
		Assert.assertEquals(SimpleReport.class, manager.reports().iterator().next().getClass());
	}

	@Test
	public void addPeriodToInstant(){
		Instant i = Instant.now();
		Period p = Period.ofMonths(-1);
		LocalDateTime dt = LocalDateTime.ofInstant(i, ZoneId.of("Europe/Berlin"));
		//i = Instant.parse("2016-10-01T00:00:00Z");
		dt.plus(p);
	}

	@Test
	public void timestampStringWithLocalOffset(){
		Instant i = Instant.now();
		String s = ReportExecution.formatIsoTimestamp(i, ZoneId.of("Europe/Berlin"));
		// may be daylight saving time
		Assert.assertTrue(s.endsWith("+01:00") || s.endsWith("+02:00"));
	}

	public static void main(String[] args) throws SQLException, XMLStreamException, ClassNotFoundException, IOException{
		Class.forName("org.postgresql.Driver");
		LocalI2b2DataSource ds = new LocalI2b2DataSource();
		String projectId="AKTIN";
		PostgresPatientStore patients = new PostgresPatientStore();
		patients.open(ds.getConnection(), projectId);
		PostgresVisitStore visits = new PostgresVisitStore();
		visits.open(ds.getConnection(), projectId);
		ObservationFactory of = new ObservationFactoryImpl(patients, visits);
		GroupedXMLWriter w = new GroupedXMLWriter(System.out);
		
		try( I2b2ExtractorFactory extractor = new I2b2ExtractorFactory(ds, of);
				I2b2Extractor ext = extractor.extract(Timestamp.valueOf("2016-12-01 00:00:00"), Timestamp.valueOf("2017-01-01 00:00:00"), null) ){
			ext.stream().forEach(w);
		}
		w.close();
		patients.close();
		visits.close();
	}
}
