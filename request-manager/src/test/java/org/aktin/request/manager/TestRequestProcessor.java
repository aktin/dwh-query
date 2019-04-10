package org.aktin.request.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;

import org.aktin.broker.query.xml.QueryRequest;
import org.aktin.broker.request.RequestStatus;
import org.aktin.scripting.r.TestRScript;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestRequestProcessor {
	private LocalHSQLDataSource ds;
	private Path rExecPath;
	private ZoneId tz;
	private RequestProcessor rp;
	private TestRequestStoreImpl rs;

	public TestRequestProcessor(){
		this.ds = new LocalHSQLDataSource();
		this.rExecPath = TestRScript.findR();
		Assert.assertNotNull(rExecPath);
		this.tz = ZoneId.systemDefault();
	}
	@Before
	public void createDatabase() throws SQLException, IOException {
		try( InputStream in = TestRequestProcessor.class.getResourceAsStream("/demo_crcdata.sql");
				BufferedReader rd = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)) )
		{
			ds.create(rd);
			int count = ds.executeCountQuery("SELECT COUNT(*) FROM visit_dimension WHERE start_date >= '2010-03-01 01:00:00' AND start_date < '2011-03-01 01:00:00'");
			// make sure we have data to work with
			Assert.assertEquals(5, count);
		}
	}

	@After
	public void dropDatabase() throws SQLException {
		ds.shutdown();
		
	}

	private void newRequestProcessor() {
		rp = new RequestProcessor();
		rp.initializeManual(this.ds, rExecPath, tz, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()));
		// no parallelization for tests
		rp.setExecutor(new Executor() {
			@Override
			public void execute(Runnable command) {
				command.run();
			}
		});
	}
	private void newRequestStore() throws IOException, SQLException, JAXBException {
		rs = new TestRequestStoreImpl();
		rs.createDirectories();
		rs.initialize();
	}

	@Test
	public void verifyQueryRequestParsing() {
		QueryRequest qr = JAXB.unmarshal(TestRequestProcessor.class.getResource("/query.xml"), QueryRequest.class);
		Assert.assertEquals(123, qr.getId());
		
	}
	@Test
	public void verifySqlPlusRScriptExecution() throws IOException, SQLException, JAXBException {

		newRequestProcessor();
		newRequestStore();
	
		try {
			QueryRequest qr = JAXB.unmarshal(TestRequestProcessor.class.getResource("/query.xml"), QueryRequest.class);
			
			RequestImpl ri = rs.addNewRequest(qr);
			rp.accept(ri);
			Assert.assertEquals(RequestStatus.Completed, ri.getStatus());
		}finally {
			rs.cleanDirectories();
		}
	}
}
