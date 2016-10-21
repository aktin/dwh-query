package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Logger;

import org.aktin.report.Report;
import org.aktin.report.aktin.AktinMonthly;
import org.junit.Assert;
import org.junit.Test;

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
	
}
