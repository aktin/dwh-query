package org.aktin.generic.imports.manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes predefined import stats queries for P21 data sources.
 * <p>
 * This class builds and runs SQL queries to fetch yearly import counts for different types of P21 data (FAB, FALL, ICD, OPS).
 */
public class P21StatsQueryExecutor {

  private final Connection connection;

  public P21StatsQueryExecutor(Connection connection) {
    this.connection = connection;
  }

  /**
   * Builds a SQL query to count distinct encounters per year from the {@code observation_fact} table
   * based on the provided filter condition.
   *
   * @param queryFilter the WHERE clause to filter data (e.g., concept code prefixes)
   * @return a complete SQL query string
   */
  private String buildQuery(String queryFilter) {
    return "SELECT date_part('year', of.import_date) AS year, " +
        "COUNT(DISTINCT of.encounter_num) AS count " +
        "FROM visit_dimension vd " +
        "JOIN observation_fact of ON vd.encounter_num = of.encounter_num " +
        "WHERE " + queryFilter + " " +
        "GROUP BY year";
  }

  /**
   * Executes the given SQL query and maps the results into a list of {@link P21ImportStats}.
   *
   * @param sql the SQL query to execute
   * @param sourceName the logical name of the data source (e.g., "FAB", "ICD")
   * @return a list of statistics objects for the specified data source
   * @throws SQLException if an error occurs during SQL execution
   */
  private List<P21ImportStats> executeQuery(String sql, String sourceName) throws SQLException {
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

  /**
   * Executes a filtered query and fetches statistics for the specified data source.
   *
   * @param sourceName the name of the data source (e.g., "FALL", "OPS")
   * @param queryFilter the WHERE clause to use in the SQL query
   * @return list of import statistics for the given filter and source
   * @throws SQLException if an error occurs while querying
   */
  private List<P21ImportStats> fetchStats(String sourceName, String queryFilter) throws SQLException {
    String sql = buildQuery(queryFilter);
    return executeQuery(sql, sourceName);
  }

  /**
   * Fetches import statistics for FAB data (encounters/admissions).
   *
   * @return a list of yearly import statistics for FAB data
   * @throws SQLException if an error occurs during the query
   */
  public List<P21ImportStats> fetchFabStats() throws SQLException {
    return fetchStats("FAB", "of.provider_id = 'P21' AND of.concept_cd LIKE 'P21:DEP%'");
  }

  /**
   * Fetches import statistics for FALL data (encounters/admissions).
   *
   * @return a list of yearly import statistics for FALL data
   * @throws SQLException if an error occurs during the query
   */
  public List<P21ImportStats> fetchFallStats() throws SQLException {
    return fetchStats("FALL", "of.provider_id = 'P21' AND of.concept_cd LIKE 'P21:ADMC%'");
  }

  /**
   * Fetches import statistics for ICD data (encounters/admissions).
   *
   * @return a list of yearly import statistics for ICD data
   * @throws SQLException if an error occurs during the query
   */
  public List<P21ImportStats> fetchIcdStats() throws SQLException {
    return fetchStats("ICD", "of.provider_id = 'P21' AND of.concept_cd LIKE 'ICD10GM%'");
  }

  /**
   * Fetches import statistics for OPS data (encounters/admissions).
   *
   * @return a list of yearly import statistics for OPS data
   * @throws SQLException if an error occurs during the query
   */
  public List<P21ImportStats> fetchOpsStats() throws SQLException {
    return fetchStats("OPS", "of.provider_id = 'P21' AND of.concept_cd LIKE 'OPS%'");
  }
}
