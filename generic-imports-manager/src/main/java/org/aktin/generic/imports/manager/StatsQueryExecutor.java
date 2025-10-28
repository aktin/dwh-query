package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Executes parameterized SQL queries ({@link QueryDef}) and returns each row as a {@code Map<String,Object>}
 */
public class StatsQueryExecutor {

  private static final Logger LOGGER = Logger.getLogger(StatsQueryExecutor.class.getName());

  private final DataSource dataSource;
  private final int queryTimeoutSeconds;

  public StatsQueryExecutor(DataSource dataSource, int queryTimeoutSeconds) {
    if (dataSource == null) {
      throw new IllegalArgumentException("DataSource required");
    }
    this.dataSource = dataSource;
    this.queryTimeoutSeconds = Math.max(0, queryTimeoutSeconds);
  }

  /**
   * Executes one SQL query and returns all rows as ordered maps. Adds {@code source} if the result lacks that column.
   *
   * @param q query definition (SQL, params, logical name)
   * @return list of rows with column labels as keys
   * @throws SQLException if execution or mapping fails
   */
  public QueryResult run(QueryDef q) throws SQLException {
    try (Connection c = dataSource.getConnection(); PreparedStatement ps = prepareAndBind(c, q); ResultSet rs = ps.executeQuery()) {
      ResultSetMetaData md = rs.getMetaData();
      int colCount = md.getColumnCount();
      List<String> columns = new ArrayList<>(colCount);
      for (int i = 1; i <= colCount; i++) {
        columns.add(md.getColumnLabel(i));
      }
      List<Map<String, Object>> rows = new ArrayList<>();
      while (rs.next()) {
        Map<String, Object> m = new HashMap<>(colCount * 2);
        for (int i = 1; i <= colCount; i++) {
          m.put(columns.get(i - 1), rs.getObject(i));
        }
        rows.add(m);
      }
      return new QueryResult(q.name, rows, columns);
    } catch (SQLTimeoutException te) {
      LOGGER.log(Level.WARNING, "Query timed out after {0}s: {1}", new Object[]{queryTimeoutSeconds, q.name});
      throw te;
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Query failed: " + q.name, e);
      throw e;
    }
  }

  /**
   * Builds a {@link PreparedStatement}, sets query timeout, and binds parameters. Parameters are bound in positional order.
   *
   * @param c open JDBC connection
   * @param q query definition
   * @return prepared and bound statement
   * @throws SQLException if preparation or binding fails
   */
  private PreparedStatement prepareAndBind(Connection c, QueryDef q) throws SQLException {
    PreparedStatement ps = c.prepareStatement(q.sql);
    if (queryTimeoutSeconds > 0) {
      ps.setQueryTimeout(queryTimeoutSeconds);
    }
    List<Object> params = q.params == null ? Collections.emptyList() : q.params;
    for (int i = 0; i < params.size(); i++) {
      ps.setObject(i + 1, params.get(i));
    }
    return ps;
  }
}
