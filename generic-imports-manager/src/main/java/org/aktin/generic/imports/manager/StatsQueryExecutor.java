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
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;

/**
 * Executes parameterized SQL queries defined by {@link QueryDef} and maps rows to JSON-friendly {@code Map<String,Object>} structures.
 * <p>
 * Resolves the JDBC {@link DataSource} from preferences in production or accepts an injected DataSource in tests.
 * <p>
 * Annotated with {@code @Stateless} for container-managed lifecycle and pooling, and {@code @TransactionAttribute(SUPPORTS)} to join an existing transaction or run without one for read-only queries.
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Stateless
public class StatsQueryExecutor {

  private static final Logger LOGGER = Logger.getLogger(StatsQueryExecutor.class.getName());

  private final DataSource dataSource;
  private final int queryTimeoutSeconds;

  /**
   * Container-managed constructor. Looks up the DataSource via JNDI name from {@link Preferences}
   *
   * @param preferences application preferences
   * @throws NamingException if JNDI lookup fails
   */
  @Inject
  public StatsQueryExecutor(Preferences preferences) throws NamingException {
    String dataSourceName = preferences.get(PreferenceKey.i2b2DatasourceCRC);
    LOGGER.info("Initializing DataSource via JNDI: " + dataSourceName);
    InitialContext ctx = new InitialContext();
    this.dataSource = (DataSource) ctx.lookup(dataSourceName);
    this.queryTimeoutSeconds = 60;
  }

  /**
   * Test-only constructor
   *
   * @param dataSource          DataSource to use
   * @param queryTimeoutSeconds JDBC query timeout in seconds
   */
  public StatsQueryExecutor(DataSource dataSource, int queryTimeoutSeconds) {
    this.dataSource = dataSource;
    this.queryTimeoutSeconds = queryTimeoutSeconds;
  }

  /**
   * Executes the given query and returns rows as ordered maps keyed by column label
   * <p>
   * Injects the {@code source} field from {@link QueryDef#getName()} if not provided by SQL
   *
   * @param q query definition (SQL + params)
   * @return list of rows (column label -> value)
   * @throws SQLException if a database error occurs (includes timeouts)
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
   * Prepares a {@link PreparedStatement} for the given query and binds all parameters. Sets {@link PreparedStatement#setQueryTimeout(int)} when configured.
   *
   * @param c JDBC connection
   * @param q query definition
   * @return prepared and bound statement
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
   * Reads all rows from the {@link ResultSet} and returns them as ordered maps. If the SQL did not produce a "source" column, adds it using {@code fallbackSource}.
   *
   * @param rs             result set positioned before the first row
   * @param fallbackSource value to use for "source" when the column is absent
   * @return list of rows
   * @throws SQLException if reading metadata or rows fails
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
