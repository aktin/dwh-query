package org.aktin.request.manager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import org.aktin.broker.query.xml.QueryRequest;
import org.aktin.broker.query.xml.TestRequest;
import org.aktin.broker.request.QueryRuleAction;
import org.aktin.dwh.db.TestDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class TestQueryRuleImpl {
	TestDataSource ds;
	private static final String SHA_1 = "SHA-1";

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
		assertEquals(0, countRulesInDatabase());
	}
	private int countRulesInDatabase() throws SQLException{
		AtomicInteger count = new AtomicInteger(0);
		try( Connection dbc = ds.getConnection() ){
			QueryRuleImpl.loadAll(dbc, r -> count.incrementAndGet() );
		}		
		return count.get();
	}

	@Test
	public void expectRuleAdded() throws SQLException, NoSuchAlgorithmException, IOException{
		QueryRequest q = TestRequest.getRepeatingRequest(100, 1);
		
//		RequestIm
		QueryRuleImpl rule;
		try( Connection dbc = ds.getConnection() ){
			// default rule
			rule = QueryRuleImpl.createRule(dbc, null, "U1", QueryRuleAction.ACCEPT_EXECUTE, SHA_1);
			rule = QueryRuleImpl.createRule(dbc, q, "U2", QueryRuleAction.ACCEPT_EXECUTE, SHA_1);
			System.out.println("Sig:"+Base64.getEncoder().encodeToString(rule.signature));
			q.getQuery().title = "Changed";
			// should fail, rule with same query id already present
			try{
				rule = QueryRuleImpl.createRule(dbc, q, "U3", QueryRuleAction.ACCEPT_EXECUTE, SHA_1);
				fail("Creating rule with duplicate query id should fail!");
			}catch( SQLException e ){
				// exception as expected
			}
		}
		assertEquals(2, countRulesInDatabase());

		// delete default rule
		try( Connection dbc = ds.getConnection() ){
			boolean r = QueryRuleImpl.deleteRule(dbc, null);
			assertTrue(r); // rule should have been deleted
		}
		assertEquals(1, countRulesInDatabase());

		// delete non-existing rule
		try( Connection dbc = ds.getConnection() ){
			boolean r = QueryRuleImpl.deleteRule(dbc, 999);
			assertFalse(r);
		}
		assertEquals(1, countRulesInDatabase());			

		// delete normal rule
		try( Connection dbc = ds.getConnection() ){
			boolean r = QueryRuleImpl.deleteRule(dbc, q.getQueryId());
			assertTrue(r);
		}
		assertEquals(0, countRulesInDatabase());			

	}

	// TODO more tests for adding/deleting

}
