package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Logger;

import org.aktin.report.Report;
import org.aktin.report.aktin.AktinMonthly;

public class TestAktinMonthly {
	private static final Logger log = Logger.getLogger(TestAktinMonthly.class.getName());	


	public static void main(String[] args) throws IOException{
		// needed to locate the Rscript binary for testing
		TestReportGeneration.locateR();
		
		Report report = new AktinMonthly();
		Path dest = Files.createTempFile("report", ".pdf");

		TestReportGeneration.generatePDF(report, Instant.parse("2015-01-01T00:00:00Z"), Instant.parse("2015-01-01T00:00:00Z"), dest);
		
		log.info("Generated file: "+dest);

	}
}
