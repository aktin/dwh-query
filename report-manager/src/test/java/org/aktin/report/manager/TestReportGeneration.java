package org.aktin.report.manager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import org.aktin.dwh.prefs.impl.TestPreferences;
import org.aktin.report.Report;
import org.aktin.report.ReportInfo;
import org.aktin.report.test.SimpleReport;
import org.aktin.scripting.r.TestRScript;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.sekmi.histream.Observation;
import de.sekmi.histream.ObservationException;

public class TestReportGeneration {

	public static Path rScript;

	@BeforeClass
	public static void locateR(){
		rScript = TestReportGeneration.findR();
	}

	public static Path findR() {
		return TestRScript.findR();
	}
	public static Path getReportTempDir(){
		Path tempDir = Paths.get("target/report-temp");
		try {
			Files.createDirectories(tempDir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return tempDir;
	}
	public static void generatePDF(Report report, Instant start, Instant end, Path pdf, TestExport dataset, boolean keepIntermediateFiles) throws IOException{
		Objects.requireNonNull(rScript, "Please call TestReportGenerator.locateR() to locate Rscript");
		ReportManagerImpl manager = new ReportManagerImpl(rScript.toString(), getReportTempDir(), report);
		ReportInfo info = report.createReportInfo(start, end);
		manager.setExecutor(ForkJoinPool.commonPool());
		manager.setKeepIntermediateFiles(keepIntermediateFiles);
		manager.setPreferenceManager(TestPreferences.getTestPreferences());
		manager.setDataExtractor(dataset);
		try {
			manager.generateReport(info, pdf).get();
		} catch (InterruptedException e) {
			// will not happen during testing
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			if( e.getCause() != null && e.getCause() instanceof ObservationException ){
				Observation o = ((ObservationException)e.getCause()).getObservation();
				System.err.println();
				if( o.getValue() != null ) {
					System.err.println("Value: "+o.getValue());
				}
			}else if( e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause() instanceof ObservationException ){
				Observation o = ((ObservationException)e.getCause().getCause()).getObservation();
				System.err.println(o);
				if( o.getValue() != null ) {
					System.err.println("Value: "+o.getValue());
				}
			}			
			if( e.getCause() instanceof IOException ){
				throw (IOException)e.getCause();
			}else {
				throw new IOException(e.getCause());
			}
		}
	}

	@Test
	public void verifyNonEmptySampleReportPDF() throws Exception{
		Report report = new SimpleReport();
		Path dest = Files.createTempFile("report", ".pdf");

		generatePDF(report, Instant.parse("2015-01-01T00:00:00Z"), Instant.parse("2015-01-01T00:00:00Z"), dest, TestExport.small(), false);
		
		// expect report written (will always be true, because createTempFile will create the file)
		Assert.assertTrue(Files.exists(dest));
		// expect non-empty file
		Assert.assertTrue(Files.size(dest) > 0);

		System.out.println("Created PDF "+dest);
		//Files.delete(dest); TODO Removed for Testing
	}

	// TODO write test cases which expect failure on FOP error or R error

//	public static void main(String args[]){
//		
//	}
}
