package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.aktin.dwh.PreferenceKey;
import org.aktin.Preferences;

@Singleton
public class P21StatsQueryManagerService {

  private static final Logger LOG = Logger.getLogger(P21StatsQueryManagerService.class.getName());

  private Map<String, List<P21ImportStats>> statsBySource;

  private P21ImportStatsQueryManager manager;

  private DataSource dataSource;

  @Inject
  private Preferences preferences;

  @Inject
  public P21StatsQueryManagerService(Preferences preferences) throws SQLException, NamingException {
    this.setPreferences(preferences);
    this.startUp();
  }

  public P21StatsQueryManagerService(Connection connection) throws SQLException {
    this.manager = new P21ImportStatsQueryManager(connection);
    this.statsBySource = new HashMap<>();
    putStats();
  }

  @Inject
  public void setPreferences(Preferences preferences) {
    this.preferences = preferences;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  private void startUp() throws SQLException, NamingException {
    if (this.dataSource == null) {
      assert preferences != null;
      String dataSourceName = preferences.get(PreferenceKey.i2b2DatasourceCRC);
      LOG.info("Initializing data source " + dataSourceName);
      InitialContext initialContext = new InitialContext();
      this.dataSource = (DataSource) initialContext.lookup(dataSourceName);
    }
    this.manager = new P21ImportStatsQueryManager(dataSource.getConnection());
    this.statsBySource = new HashMap<>();
    putStats();
  }

  @PostConstruct
  private void putStats() throws SQLException {
    this.statsBySource.put("FAB", this.manager.fetchFabStats());
    this.statsBySource.put("FALL", this.manager.fetchFallStats());
    this.statsBySource.put("OPS", this.manager.fetchOpsStats());
    this.statsBySource.put("ICD", this.manager.fetchIcdStats());
  }

  public List<P21ImportStats> getStatsBySource(String source) throws SQLException {
    return statsBySource.get(source);
  }

}
