package org.aktin.request.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.EnumSet;

import javax.xml.bind.JAXBException;

import org.aktin.broker.query.xml.QueryRequest;
import org.aktin.broker.query.xml.TestRequest;
import org.aktin.broker.request.ActionLogEntry;
import org.aktin.broker.request.RequestStatus;
import org.aktin.dwh.db.TestDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class TestRequestStoreImpl extends RequestStoreImpl{
	TestDataSource ds;

	public TestRequestStoreImpl() throws SQLException{
		super(new TestDataSource(), Paths.get("target/test-reports"));
	}


	@Before
	public void createDirectories() throws IOException{
		Files.createDirectory(getResultDir());	
	}
	@Before
	public void initialize() throws SQLException, JAXBException{
		ds = new TestDataSource();
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
	}
	@After
	public void cleanDirectories() throws IOException{
		deleteDirWithFiles(getResultDir());
	}
	@After
	public void dropDatabase() throws Exception{
		ds.dropAll();
	}

	@Override
	protected void afterRequestStatusChange(RequestImpl request, String description) {
		System.out.println("Request Status change: "+request.getRequestId()+" to "+request.getStatus());
	}

	@Test
	public void expectOneRequestAfterAddingOne() throws SQLException, JAXBException{
		QueryRequest qr = TestRequest.getSingleRequest(1);
		addNewRequest(qr);
		assertEquals(1, getRequests().size());
		reloadRequests();
		assertEquals(1, getRequests().size());
	}

	@Test
	public void deleteResultData() throws SQLException, IOException, JAXBException{
		QueryRequest qr = TestRequest.getSingleRequest(1);
		RequestImpl r = addNewRequest(qr);
		r.createResultData("test/1");
		// first delete without writing the data
		r.removeResultData();
		assertNull(r.getResultData());
		// create again - this time with data
		r.createResultData("test/2");
		try( OutputStream out = r.getResultData().getOutputStream() ){
			out.write(42);
		}
		assertNotNull(r.getResultData());
		r.removeResultData();
		assertNull(r.getResultData());

		// read back
		reloadRequests();
		// should still be null
		assertNull(r.getResultData());
	}

	@Test
	public void addAndVerifyResult() throws SQLException, IOException, JAXBException{
		QueryRequest qr = TestRequest.getSingleRequest(1);
		RequestImpl r = addNewRequest(qr);
		r.changeStatus("rm", RequestStatus.Seen, null);
		r.changeStatus(null, RequestStatus.Processing, null);

		long ts = r.getLastActionTimestamp();
		assertNotEquals(0, ts); // timestamp should set by changeStatus and should be non-zero

		r.createResultData("application/octet-stream");
		try( OutputStream out = r.getResultData().getOutputStream() ){
			out.write(42);
		}
		assertFalse(r.hasAutoSubmit());

		// should not be able to write again to the data?
		reloadRequests();
		r = getRequests().get(0);
		
		// verify status
		assertEquals(RequestStatus.Processing, r.getStatus());
		// verify last action timestamp
		assertEquals(ts, r.getLastActionTimestamp()); // timestamp should be the same as previously read

		// verify status log
		int count = 0;
		for( ActionLogEntry entry : r.getActionLog() ) {
			assertNotNull(entry);
			count ++;
		}
		assertEquals(3, count); // retrieved, seen, processing
		
		// verify content type
		assertEquals("application/octet-stream", r.getResultData().getContentType());
		// verify result content
		try( InputStream in = r.getResultData().getInputStream() ){
			assertEquals(42, in.read());
			// expect EOF
			assertEquals(-1,in.read());
		}

		// change status again
		r.changeStatus(null, RequestStatus.Completed, null);
		// should be visible in action log
		assertEquals(4, r.getActionLog().spliterator().getExactSizeIfKnown());
		
	}

}
