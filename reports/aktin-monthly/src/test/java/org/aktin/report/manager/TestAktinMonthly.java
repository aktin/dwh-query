package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

import org.aktin.Preferences;
import org.aktin.dwh.prefs.impl.TestPreferences;
import org.aktin.report.Report;
import org.aktin.report.aktin.AktinMonthly;
import org.junit.Assert;
import org.junit.Test;

import de.sekmi.histream.export.config.ExportDescriptor;
import de.sekmi.histream.i2b2.PostgresPatientStore;
import de.sekmi.histream.i2b2.PostgresVisitStore;
import de.sekmi.histream.impl.ObservationFactoryImpl;

public class TestAktinMonthly {
	private static final Logger log = Logger.getLogger(TestAktinMonthly.class.getName());	

	
	@Test
	public void testAKTINreportMax() throws IOException{
		// needed to locate the Rscript binary for testing
		log.info("Running AKTIN Monthly Report Test (Big Data Set)");
		TestReportGeneration.locateR();
		
		Report report = new AktinMonthly();
		Path dest = Files.createTempFile("report", ".pdf");

		TestReportGeneration.generatePDF(report, Instant.parse("2015-11-01T00:00:00Z"), Instant.parse("2015-12-01T00:00:00Z"), dest, TestExport.large(), true);
		
		log.info("Generated file: "+dest);
		
		// expect report written (will always be true, because createTempFile will create the file)
		Assert.assertTrue(Files.exists(dest));
		// expect non-empty file
		Assert.assertTrue(Files.size(dest) > 0);

		//Files.delete(dest);

	}
	
	@Test
	public void testAKTINreportMin() throws IOException{
		// needed to locate the Rscript binary for testing
		log.info("Running AKTIN Monthly Report Test  (Minimal Data Set)");
		TestReportGeneration.locateR();
		
		Report report = new AktinMonthly();
		Path dest = Files.createTempFile("report", ".pdf");

		TestReportGeneration.generatePDF(report, Instant.parse("2015-11-01T00:00:00Z"), Instant.parse("2015-12-01T00:00:00Z"), dest, TestExport.small(), true);
		
		log.info("Generated file: "+dest);
		
		// expect report written (will always be true, because createTempFile will create the file)
		Assert.assertTrue(Files.exists(dest));
		// expect non-empty file
		Assert.assertTrue(Files.size(dest) > 0);

		//Files.delete(dest);

	}

	public static void main(String[] args) throws SQLException, IOException, InterruptedException, ExecutionException{
		AktinMonthly m = new AktinMonthly();
		Preferences prefs = TestPreferences.getTestPreferences();
		ExportDescriptor ed = ExportDescriptor.parse(m.getExportDescriptor());
		Path rScript = TestReportGeneration.findR();
		Path tempdir = Paths.get("target/report-temp");
		LocalI2b2DataSource ds = new LocalI2b2DataSource();
		ReportManagerImpl manager = new ReportManagerImpl(rScript.toString(), tempdir, m);
		PostgresPatientStore pas = new PostgresPatientStore();
		pas.open(ds.getConnection(), "AKTIN");
		PostgresVisitStore vis = new PostgresVisitStore();
		vis.open(ds.getConnection(), "AKTIN");
		ObservationFactoryImpl fac = new ObservationFactoryImpl(pas,vis);
		DataExtractorImpl de = new DataExtractorImpl(ds, pas, vis, fac);
		de.setExecutor(ForkJoinPool.commonPool());
		manager.setExecutor(ForkJoinPool.commonPool());
		manager.setKeepIntermediateFiles(true);
		manager.setPreferenceManager(prefs);
		manager.setDataExtractor(de);
		CompletableFuture<?> cf = manager.generateReport(m.createReportInfo( Instant.parse("2016-12-01T00:00:00Z"), Instant.parse("2017-01-01T00:00:00Z")), Files.createTempFile(tempdir, "monthly", ".pdf"));
		cf.get();
		
	}
	
}
