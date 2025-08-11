package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

/**
 * Executes parameterized SQL queries ({@link QueryDef}) and returns each row as a {@code Map<String,Object>}. Uses {@code @TransactionAttribute(SUPPORTS)} so methods join an existing TX or run
 * without one (read-only).
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class StatsQueryExecutor {

  private static final Logger LOGGER = Logger.getLogger(StatsQueryExecutor.class.getName());

  private DataSource dataSource;
  private int queryTimeoutSeconds;

  public StatsQueryExecutor(DataSource dataSource, int queryTimeoutSeconds) {
    this.dataSource = dataSource;
    this.queryTimeoutSeconds = queryTimeoutSeconds;
  }

  /**
   * Executes the query and maps all rows.
   *
   * @param q SQL + params + logical name
   * @return list of rows (column label -> value); adds {@code source} if not provided by SQL
   * @throws SQLException on database error (includes timeouts)
   */
  public List<Map<String, Object>> run(QueryDef q) throws SQLException {
    long t0 = System.currentTimeMillis();
    LOGGER.info(() -> "Executing query [" + q.getName() + "]");
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = prepareAndBind(c, q);
        ResultSet rs = ps.executeQuery()) {
      return mapAllRows(rs, q.getName());
    } catch (SQLTimeoutException te) {
      long took = System.currentTimeMillis() - t0;
      LOGGER.log(Level.WARNING, "Query timed out after {0}s: [{1}] (took {2} ms)", new Object[]{queryTimeoutSeconds, q.getName(), took});
      throw te;
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Query failed: " + q.getName(), e);
      throw e;
    }
  }

  /**
   * Creates a {@link PreparedStatement}, sets {@code setQueryTimeout}, and binds parameters in order.
   *
   * @param c JDBC connection
   * @param q query definition
   * @return prepared, bound statement
   * @throws SQLException if preparing or binding fails
   */
  private PreparedStatement prepareAndBind(Connection c, QueryDef q) throws SQLException {
    PreparedStatement ps = c.prepareStatement(q.getSql());
    if (queryTimeoutSeconds > 0) {
      ps.setQueryTimeout(queryTimeoutSeconds);
    }
    int i = 1;
    for (Object p : q.getParams()) {
      ps.setObject(i++, p);
    }
    return ps;
  }

  /**
   * Iterates the {@link ResultSet} and builds ordered maps for each row. Adds {@code source=fallbackSource} when the SQL result has no {@code source} column.
   *
   * @param rs             result set
   * @param fallbackSource value for {@code source} if missing
   * @return mapped rows
   * @throws SQLException on metadata or read errors
   */
  private List<Map<String, Object>> mapAllRows(ResultSet rs, String fallbackSource) throws SQLException {
    List<Map<String, Object>> rows = new ArrayList<>();
    ResultSetMetaData md = rs.getMetaData();
    int cols = md.getColumnCount();
    while (rs.next()) {
      Map<String, Object> row = new LinkedHashMap<>();
      for (int col = 1; col <= cols; col++) {
        String key = md.getColumnLabel(col);
        if (key == null || key.isEmpty()) {
          key = md.getColumnName(col);
        }
        row.put(key, rs.getObject(col));
      }
      if (!row.containsKey("source")) {
        row.put("source", fallbackSource);
      }
      rows.add(row);
    }
    return rows;
  }
}
