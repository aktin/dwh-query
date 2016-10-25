package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Logger;

import org.aktin.report.Report;
import org.aktin.report.aktin.DataQuality;
import org.junit.Assert;
import org.junit.Test;

public class TestQualityReport {
	private static final Logger log = Logger.getLogger(TestQualityReport.class.getName());	

	
	@Test
	public void testDataQualityReportMax() throws IOException{
		// needed to locate the Rscript binary for testing
		log.info("Running Data Quality Report Test (Big Data Set)");
		TestReportGeneration.locateR();
		
		Report report = new DataQuality();
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
	public void testDataQualityReportMin() throws IOException{
		// needed to locate the Rscript binary for testing
		log.info("Running Data Quality Report Test  (Minimal Data Set)");
		TestReportGeneration.locateR();
		
		Report report = new DataQuality();
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
