package org.aktin.request.manager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.function.Consumer;

import org.aktin.broker.request.BrokerQueryRule;
import org.aktin.broker.request.QueryRuleAction;

public class QueryRuleImpl implements BrokerQueryRule {
	Integer queryId;
	String userId;
	Instant timestamp;
	String algorithm;
	byte[] signature;
	QueryRuleAction action;

	@Override
	public Integer getQueryId() {
		return queryId;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	@Override
	public Instant getTimestamp() {
		return timestamp;
	}

	@Override
	public String getSignatureAlgorithm() {
		return algorithm;
	}

	@Override
	public byte[] getSignatureData() {
		return signature;
	}

	@Override
	public QueryRuleAction getAction() {
		return action;
	}

	private static QueryRuleAction parseAction(String value){
		return QueryRuleAction.valueOf(value);
	}

	static void createRule(Connection dbc, Integer queryId, String userId, QueryRuleAction action) throws SQLException{
		final String sql = "INSERT INTO broker_query_rules(broker_query_id, create_time, create_user, action, signature_algo, signature_data)VALUES(?,?,?,?,?,?)";
		try( PreparedStatement ps = dbc.prepareStatement(sql) ){
			if( queryId == null ){
				ps.setNull(1, xxx);
			}else{
				ps.setInt(1, queryId);
			}
			
		}
	}

	static void deleteRule(Connection dbc, Integer queryId) throws SQLException{
		try( Statement st = dbc.createStatement() ){
			if( queryId == null ){
				st.executeUpdate("DELETE FROM broker_query_rules WHERE broker_query_id IS NULL");
			}else{
				st.executeUpdate("DELETE FROM broker_query_rules WHERE broker_query_id="+queryId);
			}
		}
	}
	static void loadAll(Connection dbc, Consumer<QueryRuleImpl> action) throws SQLException{
		try( Statement st = dbc.createStatement();
				ResultSet rs = st.executeQuery(
						"SELECT broker_query_id, create_time, create_user"
						+ " action, signature_algo, signature_data, comment"
						+ " FROM broker_query_rules") ){
			while( rs.next() ){
				QueryRuleImpl r = new QueryRuleImpl();
				// load other data
				Integer id = rs.getInt(1);
				if( rs.wasNull() ){
					id = null;
				}
				r.queryId = id;
				
				r.timestamp = rs.getTimestamp(2).toInstant();
				r.userId = rs.getString(3);
				r.action = parseAction( rs.getString(4) );
				r.algorithm = rs.getString(5);
				r.signature = rs.getBytes(6);
				// comment
				rs.getString(7);
				
				action.accept(r);
			}
		}
	}

}
