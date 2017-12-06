package org.aktin.request.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.aktin.dwh.db.TestDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class TestQueryRuleImpl {
	TestDataSource ds;

	public TestQueryRuleImpl() throws SQLException{
		ds = new TestDataSource();
	}

	@Before
	public void initialize() throws SQLException{
		ds = new TestDataSource();
	}

	@After
	public void dropDatabase() throws Exception{
		ds.dropAll();
	}

	@Test
	public void expectNoRulesOnStart() throws SQLException{
		AtomicInteger count = new AtomicInteger(0);
		try( Connection dbc = ds.getConnection() ){
			QueryRuleImpl.loadAll(dbc, r -> count.incrementAndGet() );
		}
		assertEquals(0, count.get());
	}

	// TODO more tests for adding/deleting

}
