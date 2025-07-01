package org.aktin.generic.imports.manager.repository;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.aktin.generic.imports.manager.model.ImportStats;
import org.aktin.generic.imports.manager.strategy.ImportSourceStrategy;

public class DatabaseImportStatsRepository implements ImportStatsRepository {
  private final Connection connection;
  private final List<ImportSourceStrategy> strategies;
  public DatabaseImportStatsRepository(Connection connection, List<ImportSourceStrategy> strategies) {
    this.connection = connection;
    this.strategies = strategies;
  }


  @Override
  public List<ImportStats> getImportStats() throws Exception {
    List<ImportStats> allStats = new ArrayList<>();

    for (ImportSourceStrategy strategy : strategies) {
      List<ImportStats> statsForSource = strategy.fetchStats(connection);
      allStats.addAll(statsForSource);
    }
    allStats.sort(Comparator.comparingInt(ImportStats::getYear).thenComparing(ImportStats::getSource));
    return allStats;
  }
}
