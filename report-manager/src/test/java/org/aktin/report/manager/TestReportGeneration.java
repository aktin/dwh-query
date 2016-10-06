package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;

import org.aktin.dwh.PreferenceKey;
import org.aktin.dwh.prefs.impl.TestPreferences;
import org.aktin.report.Report;
import org.aktin.report.test.SimpleReport;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReportGeneration {

	static final String[] rPathSearch = {
			"C:\\Program Files\\R\\R-3.2.2\\bin\\Rscript.exe",
			"C:\\Program Files\\R\\R-3.2.0\\bin\\Rscript.exe"
	};
	public static Path rScript;

	@BeforeClass
	public static void locateR(){
		// try system property 'rscript.binary'
		String path = System.getProperty(PreferenceKey.rScriptBinary.key());
		if( path != null ){
			rScript = Paths.get(path);
			if( !Files.isExecutable(rScript) ){
				Assert.fail("System property '"+PreferenceKey.rScriptBinary.key()+"' defined, but target not found/executable.");
			}
			return;
		}
		// try windows path
		for( String binary : rPathSearch ){
			Path p = Paths.get(binary);
			if( Files.isExecutable(p) ){
				rScript = p;
				return;
			}
		}
		Assert.fail("Path to Rscript not found. Please edit TestReportGeneration.java or define a (local) system property: "+PreferenceKey.rScriptBinary.key());
	}

	public static void generatePDF(Report report, Instant start, Instant end, Path pdf) throws IOException{
		Objects.requireNonNull(rScript, "Please call TestReportGenerator.locateR() to locate Rscript");
		ReportManager manager = new ReportManager(rScript.toString(), report);
		manager.setPreferenceManager(TestPreferences.getTestPreferences());
		TestExport export = new TestExport();
		manager.setDataExtractor(export);
		manager.generateReport(report, start, end, pdf);

	}

	@Test
	public void verifyNonEmptySampleReportPDF() throws IOException{
		Report report = new SimpleReport();
		Path dest = Files.createTempFile("report", ".pdf");

		generatePDF(report, Instant.parse("2015-01-01T00:00:00Z"), Instant.parse("2015-01-01T00:00:00Z"), dest);
		
		// expect report written (will always be true, because createTempFile will create the file)
		Assert.assertTrue(Files.exists(dest));
		// expect non-empty file
		Assert.assertTrue(Files.size(dest) > 0);

		Files.delete(dest);
	}

	// TODO write test cases which expect failure on FOP error or R error

//	public static void main(String args[]){
//		
//	}
}
