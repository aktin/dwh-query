package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class P21ImportStatsQueryManager implements GenericQueryManager<ImportStats> {

  private final Connection connection;

  public P21ImportStatsQueryManager(Connection connection) {
    this.connection = connection;
  }

  @Override
  public String buildQuery(String queryFilter) {
    return "SELECT date_part('year', of.import_date) AS year, " +
        "COUNT(DISTINCT of.encounter_num) AS count " +
        "FROM visit_dimension vd " +
        "JOIN observation_fact of ON vd.encounter_num = of.encounter_num " +
        "WHERE " + queryFilter + " " +
        "GROUP BY year";
  }

  @Override
  public List<ImportStats> executeQuery(String sql, String sourceName) throws SQLException {
    List<ImportStats> stats = new ArrayList<>();
    try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        int year = rs.getInt("year");
        int count = rs.getInt("count");
        stats.add(new ImportStats(year, sourceName, count));
      }
    }
    return stats;
  }

  private List<ImportStats> fetchStats(String sourceName, String queryFilter) throws SQLException {
    String sql = buildQuery(queryFilter);
    return executeQuery(sql, sourceName);
  }

  public List<ImportStats> fetchFabStats() throws SQLException {
    return fetchStats("FAB", "of.provider_id = 'P21' AND of.concept_cd LIKE 'P21:DEP%'");
  }

  public List<ImportStats> fetchFallStats() throws SQLException {
    return fetchStats("FALL", "of.concept_cd LIKE 'P21:ADMC%'");
  }

  public List<ImportStats> fetchIcdStats() throws SQLException {
    return fetchStats("ICD", "of.provider_id = 'P21' AND of.concept_cd LIKE 'ICD10GM%'");
  }

  public List<ImportStats> fetchOpsStats() throws SQLException {
    return fetchStats("OPS", "of.provider_id = 'P21' AND of.concept_cd LIKE 'OPS%'");
  }
}


  /*
  class that calls fetchFabStats (and Rest) creates a map to mark the source

  1. some class calls fetchFabStats()
  2. Gets List<ImportStats>
  3. Puts List into Map like
  {
  "FAB": <Content of List<ImportStats>

   */
