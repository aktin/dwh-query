package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
  public List<Map<String, Object>> run(QueryDef q) throws SQLException {
    try (Connection c = dataSource.getConnection(); PreparedStatement ps = prepareAndBind(c, q); ResultSet rs = ps.executeQuery()) {
      return mapAllRows(rs, q.getName());
    } catch (SQLTimeoutException te) {
      LOGGER.log(Level.WARNING, "Query timed out after {0}s: {1}", new Object[]{queryTimeoutSeconds, q.getName()});
      throw te;
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING, "Query failed: " + q.getName(), e);
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
    PreparedStatement ps = c.prepareStatement(q.getSql());
    if (queryTimeoutSeconds > 0) {
      ps.setQueryTimeout(queryTimeoutSeconds);
    }
    List<Object> params = q.getParams() == null ? Collections.emptyList() : q.getParams();
    for (int i = 0; i < params.size(); i++) {
      ps.setObject(i + 1, params.get(i));
    }
    return ps;
  }

  /**
   * Converts a {@link ResultSet} into a list of ordered maps. Each map holds column label → value. If {@code source} column is missing, adds {@code source=fallbackSource}.
   *
   * @param rs             result set to map
   * @param fallbackSource value for {@code source} if missing
   * @return mapped rows
   * @throws SQLException if reading result metadata or data fails
   */
  private List<Map<String, Object>> mapAllRows(ResultSet rs, String fallbackSource) throws SQLException {
    ResultSetMetaData md = rs.getMetaData();
    int cols = md.getColumnCount();
    String[] labels = new String[cols];
    for (int i = 1; i <= cols; i++) {
      String l = md.getColumnLabel(i);
      labels[i - 1] = (l == null || l.isEmpty()) ? md.getColumnName(i) : l;
    }
    List<Map<String, Object>> rows = new ArrayList<>();
    while (rs.next()) {
      Map<String, Object> row = new LinkedHashMap<>(cols + 1);
      for (int i = 1; i <= cols; i++) {
        row.put(labels[i - 1], rs.getObject(i));
      }
      if (!row.containsKey("source")) {
        row.put("source", fallbackSource);
      }
      rows.add(row);
    }
    return rows;
  }
}
