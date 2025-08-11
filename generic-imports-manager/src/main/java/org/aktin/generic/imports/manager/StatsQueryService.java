package org.aktin.generic.imports.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Executes all queries defined by a {@link StatsSpec} using a {@link StatsQueryExecutor} and aggregates the results.
 */
@Stateless
public class StatsQueryService {

  private static final Logger LOGGER = Logger.getLogger(StatsQueryService.class.getName());

  private final StatsQueryExecutor executor;

  @Inject
  public StatsQueryService(StatsQueryExecutor executor) {
    this.executor = executor;
  }

  /**
   * Runs all queries from the specified {@link StatsSpec} and returns the combined results. Logs any {@link SQLException} and returns {@code null} if execution fails.
   *
   * @param spec statistics specification containing queries to execute
   * @return list of result rows as maps, or {@code null} if a database error occurs
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
