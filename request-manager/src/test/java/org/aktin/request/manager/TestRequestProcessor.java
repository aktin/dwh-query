package org.aktin.request.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.ZoneId;

import org.aktin.scripting.r.TestRScript;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestRequestProcessor {
	private LocalHSQLDataSource ds;
	private Path rExecPath;
	private ZoneId tz;

	public TestRequestProcessor() throws SQLException, IOException {
		this.ds = new LocalHSQLDataSource();
		try( InputStream in = TestRequestProcessor.class.getResourceAsStream("/demo_crcdata.sql");
				BufferedReader rd = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)) )
		{
			ds.create(rd);
		}
		this.rExecPath = TestRScript.findR();
		Assert.assertNotNull(rExecPath);
		this.tz = ZoneId.systemDefault();
	}
	@Before
	public void createDatabase() {
		
	}

	@After
	public void dropDatabase() throws SQLException {
		ds.shutdown();
		
	}
	@Test
	public void dummy() {
		RequestProcessor rp = new RequestProcessor();
		rp.initializeManual(this.ds, rExecPath, tz);

	}
}
