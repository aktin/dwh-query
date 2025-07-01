package org.aktin.generic.imports.manager.strategy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.aktin.generic.imports.manager.model.ImportStats;

public abstract class AbstractDbImportStrategy implements ImportSourceStrategy {

  private final String sourceName;

  protected AbstractDbImportStrategy(String sourceName) {
    this.sourceName = sourceName;
  }

  @Override
  public String getSourceName() {
    return sourceName;
  }

  protected abstract String buildCondition();

  @Override
  public List<ImportStats> fetchStats(Connection connection) throws SQLException {
    String sql =
        "SELECT date_part('year', of.import_date) AS year, " +
            "COUNT(DISTINCT of.encounter_num) AS count " +
            "FROM visit_dimension vd " +
            "JOIN observation_fact of ON vd.encounter_num = of.encounter_num " +
            "WHERE " + buildCondition() + " " +
            "GROUP BY year";
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

}
