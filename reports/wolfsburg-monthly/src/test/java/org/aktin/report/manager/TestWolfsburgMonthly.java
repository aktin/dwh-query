package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.aktin.report.Report;
import org.aktin.report.wolfsburg.WolfsburgMonthly;

public class TestWolfsburgMonthly {


	public static void main(String[] args) throws IOException{
		// needed to locate the Rscript binary for testing
		TestReportGeneration.locateR();
		
		Report report = new WolfsburgMonthly();
		Path dest = Files.createTempFile("report", ".pdf");

		TestReportGeneration.generatePDF(report, Instant.parse("2015-01-01T00:00:00Z"), Instant.parse("2015-01-01T00:00:00Z"), dest);
		
		System.out.println("Generated file: "+dest);

	}
}
