package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;

@Stateless
public class StatsQueryExecutor {

  private static final Logger LOG = Logger.getLogger(StatsQueryExecutor.class.getName());

  private final DataSource dataSource;
  private final int queryTimeoutSeconds;

  @Inject
  public StatsQueryExecutor(Preferences preferences) throws NamingException {
    String dataSourceName = preferences.get(PreferenceKey.i2b2DatasourceCRC);
    LOG.info("Initializing data source: " + dataSourceName);
    InitialContext ctx = new InitialContext();
    this.dataSource = (DataSource) ctx.lookup(dataSourceName);
    this.queryTimeoutSeconds = 60;
  }

  /**
   * Constructor for testing purposes. Initializes the executor with a custom DataSource
   */
  public StatsQueryExecutor(DataSource dataSource) {
    this.dataSource = dataSource;
    this.queryTimeoutSeconds = 60;
  }

  public List<Map<String, Object>> run(QueryDef q) throws SQLException {
    Connection c = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      c = dataSource.getConnection();
      ps = c.prepareStatement(q.getSql());
      if (queryTimeoutSeconds > 0) {
        ps.setQueryTimeout(queryTimeoutSeconds);
      }

      int i = 1;
      for (Object p : q.getParams()) {
        ps.setObject(i++, p);
      }

      rs = ps.executeQuery();
      List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
      ResultSetMetaData md = rs.getMetaData();
      int cols = md.getColumnCount();

      while (rs.next()) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        for (int col = 1; col <= cols; col++) {
          String key = md.getColumnLabel(col);
          if (key == null || key.isEmpty()) {
            key = md.getColumnName(col);
          }
          row.put(key, normalize(rs.getObject(col)));
        }
        if (!row.containsKey("source")) {
          row.put("source", q.getName());
        }
        rows.add(row);
      }
      return rows;
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (Exception ignore) {
        }
      }
      if (ps != null) {
        try {
          ps.close();
        } catch (Exception ignore) {
        }
      }
      if (c != null) {
        try {
          c.close();
        } catch (Exception ignore) {
        }
      }
    }
  }

  // make JDBC types JSON-friendly
  private Object normalize(Object v) throws SQLException {
    if (v == null) {
      return null;
    }
    if (v instanceof java.sql.Timestamp) {
      return ((Timestamp) v).toInstant().toString();
    }
    if (v instanceof java.sql.Date) {
      return ((java.sql.Date) v).toLocalDate().toString();
    }
    if (v instanceof java.sql.Time) {
      return ((java.sql.Time) v).toLocalTime().toString();
    }
    if (v instanceof java.math.BigDecimal) {
      java.math.BigDecimal bd = (java.math.BigDecimal) v;
      return bd.scale() <= 0 ? bd.longValue() : bd.doubleValue();
    }
    return v;
  }
}

