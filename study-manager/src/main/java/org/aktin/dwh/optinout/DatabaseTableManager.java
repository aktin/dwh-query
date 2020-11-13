package org.aktin.dwh.optinout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Create and manage database table structures for the study manager.
 * Primary purpose is to check for the tables and create them if not existing.
 * @author R.W.Majeed
 *
 */
public class DatabaseTableManager {
	private static final Logger log = Logger.getLogger(DatabaseTableManager.class.getName());

	private Connection dbc;

	public DatabaseTableManager(Connection dbc) {
		this.dbc = dbc;
	}

	public void checkAndCreateTables() throws IOException, SQLException {
		// check if tables exist
		try( Statement s = dbc.createStatement();
				ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM optinout_studies") ){
			rs.next();
			log.info("Tables for study_manager existing");
			return; // leave this method
		}catch( SQLException e ) {
			// tables probably do not exist
			log.info("Tables for study_manager (probably) missing");
		}

		executeSQL("/create_tables.sql");
	}

	public void addStudy(String id, String title, String description, String options, String sic_generate) throws SQLException {
		try( PreparedStatement ps = dbc.prepareStatement("INSERT INTO optinout_studies (id,title,description,created_ts,options,sic_generate) VALUES (?,?,?,NOW(),?,?)") ){
			ps.setString(1, id);
			ps.setString(2, title);
			ps.setString(3, description);
			ps.setString(4, options);
			ps.setString(5, sic_generate);
			ps.execute();
		}
	}
	private void executeSQL(String resourceName) throws IOException, SQLException {
		List<String> sql = new ArrayList<>();
		try( InputStream in = DatabaseTableManager.class.getResourceAsStream(resourceName) ){
			splitStatements(in, sql::add);
		}

		for( String ddl : sql ) {
			try( Statement s = dbc.createStatement() ){
				s.execute(ddl);
			}
		}

	}
	public void dropAllTables() throws IOException, SQLException {
		executeSQL("/drop_tables.sql");
		
	}
	/**
	 * Parse the source code into separate statements.
	 * @return sqlSource input stream containing the SQL code
	 * @throws IOException IO error
	 */
	private void splitStatements(InputStream sqlSource, Consumer<String> handler) throws IOException {
		try( BufferedReader rd = new BufferedReader(new InputStreamReader(sqlSource, StandardCharsets.UTF_8)) ){
			StringBuilder stmt = new StringBuilder();
			String line;
			while( null != (line = rd.readLine()) ) {
				line = line.trim();
				// check for comment
				if( line.startsWith("--") ){
					// found comment
					continue;
				}
				if( line.endsWith(";") ){
					// end of command
					// append to statement w/o ;
					stmt.append(line.substring(0, line.length()-1));
					handler.accept(stmt.toString());
					stmt = new StringBuilder();
				}else{
					// add the line to the statement
					stmt.append(line);
					// add whitespace which may have been stripped by xml
					stmt.append(' ');
				}
			}
		}
	}

}
