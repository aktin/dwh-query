package org.aktin.generic.imports.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Singleton;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;

/**
 * Singleton service that initializes the {@link StatsQueryExecutor} (via JNDI {@link DataSource}) and runs specs. CDI-injected constructor resolves the JNDI name from {@link Preferences}.
 */
@Singleton
public class StatsQueryService {

  private static final Logger LOGGER = Logger.getLogger(StatsQueryService.class.getName());

  private StatsQueryExecutor executor;

  public StatsQueryService() {
  }

  @Inject
  public StatsQueryService(Preferences preferences) {
    try {
      String jndi = preferences.get(PreferenceKey.i2b2DatasourceCRC);
      LOGGER.info("Initializing DataSource via JNDI: " + jndi);
      InitialContext ctx = new InitialContext();
      DataSource dataSource = (DataSource) ctx.lookup(jndi);
      this.executor = new StatsQueryExecutor(dataSource, 60);
    } catch (NamingException e) {
      throw new IllegalStateException("DataSource lookup failed", e);
    }
  }

  /**
   * Test-only constructor
   */
  public StatsQueryService(StatsQueryExecutor executor) {
    this.executor = executor;
  }

  /**
   * Runs all queries from the given spec and aggregates the results. Returns an empty list on {@link SQLException} and logs the error.
   *
   * @param spec statistics specification
   * @return list of result rows as maps, or empty list on error
   */
  public List<Map<String, Object>> run(StatsSpec spec) {
    try {
      List<Map<String, Object>> out = new ArrayList<>();
      for (QueryDef q : spec.queries()) {
        out.addAll(executor.run(q));
      }
      return out;
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Failed to execute stats queries", e);
      return new ArrayList<>();
    }
  }
}
