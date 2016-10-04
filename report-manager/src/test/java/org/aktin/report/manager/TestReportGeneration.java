package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;

import javax.tools.StandardJavaFileManager;

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
	};
	static Path rScript;

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
	

	@Test
	public void verifyNonEmptySampleReportPDF() throws IOException{
		Report report = new SimpleReport();
		ReportManager manager = new ReportManager(rScript.toString(), report);
		manager.setPreferenceManager(TestPreferences.getTestPreferences());
		TestExport export = new TestExport();
		manager.setDataExtractor(export);
		Path dest = Files.createTempFile("report", ".pdf");
		manager.generateReport(report, Instant.parse("2015-01-01T00:00"), Instant.parse("2015-01-01T00:00"), dest);
		Files.delete(dest);
	}

//	public static void main(String args[]){
//		
//	}
}
