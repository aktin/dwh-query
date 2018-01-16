package org.aktin.request.manager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import org.aktin.broker.query.xml.QueryRequest;
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

	/**
	 * Parse a database string serialization of the rule action
	 * into the enum type.
	 * @param value database string
	 * @return action enum
	 */
	private static QueryRuleAction parseAction(String value){
		Objects.requireNonNull(value);
		return QueryRuleAction.valueOf(value);
	}

	/**
	 * Convert the query rule action to database string
	 * @param action action
	 * @return database string serialization
	 */
	private static String serializeAction(QueryRuleAction action){
		
		String str = action.name();
		Objects.requireNonNull(str);
		System.out.println("Writing "+str);
		return str;
	}

	/**
	 * Create a new query rule
	 * @param dbc database connection
	 * @param req null for default rule, or request with query with non-empty id
	 * @param userId user
	 * @param action action
	 * @return newly created rule
	 * @throws SQLException sql error
	 * @throws NoSuchAlgorithmException signature algorithm not supported
	 * @throws IOException io error
	 */
	static QueryRuleImpl createRule(Connection dbc, QueryRequest req, String userId, QueryRuleAction action, String signatureAlgorithm) throws SQLException, NoSuchAlgorithmException, IOException{
		final String sql = "INSERT INTO broker_query_rules(broker_query_id, create_time, create_user, action, signature_algo, signature_data)VALUES(?,CURRENT_TIMESTAMP,?,?,?,?)";
		QueryRuleImpl rule = new QueryRuleImpl();
		rule.action = action;
		rule.userId = userId;
		Objects.requireNonNull(action);
		if( req == null ){
			rule.queryId = null;
		}else{
			rule.queryId = req.getQueryId();
			if( rule.queryId == null ){
				throw new IllegalArgumentException("queryId required for rule");
			}
			rule.algorithm = signatureAlgorithm;
			rule.signature = RequestImpl.calculateQuerySignature(req, rule.algorithm);			
		}
		try( PreparedStatement ps = dbc.prepareStatement(sql) ){
			if( rule.queryId == null ){
				ps.setNull(1, java.sql.Types.INTEGER);
			}else{
				ps.setInt(1, rule.queryId);
			}
			ps.setString(2, rule.userId);
			ps.setString(3, serializeAction(action));
			ps.setString(4, rule.algorithm);
			ps.setBytes(5, rule.signature);
			ps.executeUpdate();
		}
		return rule;
	}

	/**
	 * Delete a rule with the given query id. Query id can be null, to delete the default rule
	 * @param dbc database connection
	 * @param queryId query id or null for default rule
	 * @return {@code true} if a rule was delteted, {@code false} if no rows were affected
	 * @throws SQLException sql error
	 */
	static boolean deleteRule(Connection dbc, Integer queryId) throws SQLException{
		int count = 0;
		try( Statement st = dbc.createStatement() ){
			if( queryId == null ){
				count = st.executeUpdate("DELETE FROM broker_query_rules WHERE broker_query_id IS NULL");
			}else{
				count = st.executeUpdate("DELETE FROM broker_query_rules WHERE broker_query_id="+queryId);
			}
		}
		return( count != 0 );
	}
	static void loadAll(Connection dbc, Consumer<QueryRuleImpl> action) throws SQLException{
		try( Statement st = dbc.createStatement();
				ResultSet rs = st.executeQuery(
						"SELECT broker_query_id, create_time, create_user, "
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

	@Override
	public boolean verifySignature(QueryRequest request) throws NoSuchAlgorithmException, IOException {
		Objects.requireNonNull(request);
		Objects.requireNonNull(this.algorithm);
		Objects.requireNonNull(this.signature);
		byte[] sig = RequestImpl.calculateQuerySignature(request, this.algorithm);
		return Arrays.equals(sig, signature);
	}

}
