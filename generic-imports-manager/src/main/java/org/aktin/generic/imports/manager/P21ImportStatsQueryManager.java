package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class P21ImportStatsQueryManager {

  private final Connection connection;

  public P21ImportStatsQueryManager(Connection connection) {
    this.connection = connection;
  }

  public String buildQuery(String queryFilter) {
    return "SELECT date_part('year', of.import_date) AS year, " +
        "COUNT(DISTINCT of.encounter_num) AS count " +
        "FROM visit_dimension vd " +
        "JOIN observation_fact of ON vd.encounter_num = of.encounter_num " +
        "WHERE " + queryFilter + " " +
        "GROUP BY year";
  }

  public List<P21ImportStats> executeQuery(String sql, String sourceName) throws SQLException {
    List<P21ImportStats> stats = new ArrayList<>();
    try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        int year = rs.getInt("year");
        int count = rs.getInt("count");
        stats.add(new P21ImportStats(year, sourceName, count));
      }
    }
    return stats;
  }

  private List<P21ImportStats> fetchStats(String sourceName, String queryFilter) throws SQLException {
    String sql = buildQuery(queryFilter);
    return executeQuery(sql, sourceName);
  }

  protected List<P21ImportStats> fetchFabStats() throws SQLException {
    return fetchStats("FAB", "of.provider_id = 'P21' AND of.concept_cd LIKE 'P21:DEP%'");
  }

  protected List<P21ImportStats> fetchFallStats() throws SQLException {
    return fetchStats("FALL", "of.provider_id = 'P21' AND of.concept_cd LIKE 'P21:ADMC%'");
  }

  protected List<P21ImportStats> fetchIcdStats() throws SQLException {
    return fetchStats("ICD", "of.provider_id = 'P21' AND of.concept_cd LIKE 'ICD10GM%'");
  }

  protected List<P21ImportStats> fetchOpsStats() throws SQLException {
    return fetchStats("OPS", "of.provider_id = 'P21' AND of.concept_cd LIKE 'OPS%'");
  }
}
