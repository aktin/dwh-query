package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;

/**
 * Service class that initializes the P21StatsQueryExecutor and provides access to aggregated P21 import statistics.
 * <p>
 * Fetches data from multiple predefined sources (FAB, FALL, ICD, OPS) using SQL queries built and executed by {@link P21StatsQueryExecutor}.
 */
@Singleton
public class P21StatsQueryManagerService {

  private static final Logger LOG = Logger.getLogger(P21StatsQueryManagerService.class.getName());

  private P21StatsQueryExecutor manager;

  /**
   * Default constructor for dependency injection.
   */
  public P21StatsQueryManagerService() {

  }

  /**
   * Constructs the service using application preferences to resolve the data source via JNDI.
   *
   * @param preferences the application preferences used to look up the data source
   * @throws SQLException if a database access error occurs
   * @throws NamingException if the data source lookup fails
   */
  @Inject
  public P21StatsQueryManagerService(Preferences preferences) throws SQLException, NamingException {
    String dataSourceName = preferences.get(PreferenceKey.i2b2DatasourceCRC);
    LOG.info("Initializing data source: " + dataSourceName);
    InitialContext ctx = new InitialContext();
    DataSource dataSource = (DataSource) ctx.lookup(dataSourceName);
    this.manager = new P21StatsQueryExecutor(dataSource.getConnection());
  }

  /**
   * Constructor for testing purposes. Initializes the executor with a direct JDBC connection.
   *
   * @param connection the JDBC connection to use for querying
   */
  public P21StatsQueryManagerService(Connection connection) {
    this.manager = new P21StatsQueryExecutor(connection);
  }

  /**
   * Fetches and aggregates all available P21 import statistics from FALL, FAB, ICD, and OPS files.
   *
   * @return a combined list of {@link P21ImportStats} representing all known import statistics
   * @throws SQLException if an error occurs during query execution
   */
  public List<P21ImportStats> fetchAllP21Stats() throws SQLException {
    List<P21ImportStats> stats = new ArrayList<>();
    stats.addAll(this.manager.fetchFallStats());
    stats.addAll(this.manager.fetchFabStats());
    stats.addAll(this.manager.fetchIcdStats());
    stats.addAll(this.manager.fetchOpsStats());
    return stats;
  }
}
