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
	public void testAKTINreport() throws IOException{
		// needed to locate the Rscript binary for testing
		log.info("Running AKTIN Monthly Report Test");
		TestReportGeneration.locateR();
		
		Report report = new AktinMonthly();
		Path dest = Files.createTempFile("report", ".pdf");

		TestReportGeneration.generatePDF(report, Instant.parse("2015-11-01T00:00:00Z"), Instant.parse("2015-12-01T00:00:00Z"), dest);
		
		log.info("Generated file: "+dest);
		
		// expect report written (will always be true, because createTempFile will create the file)
		Assert.assertTrue(Files.exists(dest));
		// expect non-empty file
		Assert.assertTrue(Files.size(dest) > 0);

		//Files.delete(dest);

	}
	
	/*
	@Test
	public static void main(String[] args) throws IOException{
		// needed to locate the Rscript binary for testing
		TestReportGeneration.locateR();
		
		Report report = new AktinMonthly();
		Path dest = Files.createTempFile("report", ".pdf");

		TestReportGeneration.generatePDF(report, Instant.parse("2015-11-01T00:00:00Z"), Instant.parse("2015-12-01T00:00:00Z"), dest);
		
		log.info("Generated file: "+dest);

	}  */
}
