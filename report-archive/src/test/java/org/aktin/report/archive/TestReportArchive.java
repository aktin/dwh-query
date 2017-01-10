package org.aktin.report.archive;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;

import org.aktin.dwh.db.TestDataSource;
import org.aktin.report.ArchivedReport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import liquibase.exception.LiquibaseException;

public class TestReportArchive {

	ReportArchiveImpl me;
	Path dataDir;
	Path archiveDir;
	TestDataSource ds;

	public TestReportArchive(){
		dataDir = Paths.get("target/test-reports");
		archiveDir = Paths.get("target/test-reports-archive");
	}

	@Before
	public void createDirectories() throws IOException{
		Files.createDirectory(dataDir);
		Files.createDirectory(archiveDir);		
	}
	@Before
	public void initialize() throws IOException, SQLException{
		ds = new TestDataSource();
		me = new ReportArchiveImpl(ds, dataDir, archiveDir);
		me.loadArchive();
	}

	private static void deleteDirWithFiles(Path path) throws IOException{
		Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), 2, new FileVisitor<Path>(){

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				throw exc;
			}});
//		try( Stream<Path> files = Files.list(dir) ){
//			files.forEach(file -> {
//				try {
//				} catch (IOException e) {
//					throw new UncheckedIOException(e);
//				}
//			});
//		}catch( UncheckedIOException e ){
//			throw e.getCause();
//		}
//		Files.delete(path);
	}
	@After
	public void cleanDirectories() throws IOException{
		deleteDirWithFiles(dataDir);
		deleteDirWithFiles(archiveDir);
	}
	@After
	public void dropDatabase() throws LiquibaseException, SQLException{
		ds.dropAll();
	}
	@Test
	public void createReports() throws IOException{
		ReportInfoImpl r = new ReportInfoImpl("test", "t1");
		r.start = Instant.parse("2000-01-01T00:00:00Z");
		r.end = Instant.parse("2000-01-02T00:00:00Z");
		r.prefs.put("prop1", "val1");
		// failed report
		ArchivedReport ar = me.addReport(r, "User1");
		me.setReportFailure(ar.getId(), null, new AssertionError());
		
		// succeeded report
		ar = me.addReport(r, "User3");
		r.mediaType = "text/vnd.aktin.test";
		r.location = Files.createTempFile("report-test",".txt");
		r.dataTimestamp = Instant.now();
		// write something to the file
		try( BufferedWriter w = Files.newBufferedWriter(r.location) ){
			w.write("test-content");
		}
		me.setReportResult(ar.getId(), r);
		
		// waiting report
		ar = me.addReport(r, "User3");
		
		// reload archive
		me.loadArchive();
	}
}
