package org.aktin.generic.imports.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;

/**
 * Singleton facade to execute {@link StatsSpec} via a {@link StatsQueryExecutor}. Resolves the i2b2 {@link DataSource} from JNDI using {@link Preferences}.
 */
@ApplicationScoped
public class StatsQueryService {

  private static final Logger LOGGER = Logger.getLogger(StatsQueryService.class.getName());

  @Inject
  Preferences preferences;

  @Inject
  StatsSpecNotifier notifier;

  private StatsQueryExecutor executor;

  public StatsQueryService() {
  } // required for proxy

  @PostConstruct
  void init() {
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
  public StatsQueryService(StatsQueryExecutor ex, StatsSpecNotifier up) {
    this.executor = Objects.requireNonNull(ex);
    this.notifier = Objects.requireNonNull(up);
  }

  /**
   * Runs all queries of the given spec and returns the combined rows. On error returns an empty list and logs the cause.
   *
   * @param spec statistics specification
   * @return combined result rows, or an empty list on failure
   */
  public List<Map<String, Object>> run(StatsSpec spec) {
    Objects.requireNonNull(spec, "spec");
    try {
      List<Map<String, Object>> out = new ArrayList<>();
      for (QueryDef q : spec.queries()) {
        out.addAll(executor.run(q));
      }
      notifier.tryUpload(spec.id(), out);
      return out;
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Stats execution failed for spec: " + spec.id(), e);
      return Collections.emptyList();
    }
  }
}
