package org.aktin.generic.imports.manager.repository;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.aktin.generic.imports.manager.enums.CSVFile;
import org.aktin.generic.imports.manager.model.ImportStats;

public class ImportStatsRepository {
  private final Connection connection;

  public ImportStatsRepository(Connection connection) {
    this.connection = connection;
  }
  public List<ImportStats> getImportStats() throws SQLException {
    String sql =
        "SELECT year, source, count " +
            "FROM ( " +
            // FALL data
            "SELECT date_part('year', of.import_date) AS year, 'FALL' AS source, COUNT(DISTINCT of.encounter_num) AS count " +
            "FROM visit_dimension AS vd " +
            "JOIN observation_fact AS of ON vd.encounter_num = of.encounter_num " +
            "WHERE of.concept_cd LIKE 'P21:ADMC%'" +
            "GROUP BY year " +
            "UNION ALL " +
            // FAB data
            "SELECT date_part('year', of.import_date) AS year, 'FAB' AS source, COUNT(DISTINCT of.encounter_num) AS count " +
            "FROM visit_dimension AS vd " +
            "JOIN observation_fact AS of ON vd.encounter_num = of.encounter_num " +
            "WHERE of.provider_id = 'P21' " +
            "AND of.concept_cd LIKE 'P21:DEP%' " +
            "GROUP BY year " +
            "UNION ALL " +
            // ICD data
            "SELECT date_part('year', of.import_date) AS year, 'ICD' AS source, COUNT(DISTINCT of.encounter_num) AS count " +
            "FROM visit_dimension AS vd " +
            "JOIN observation_fact AS of ON vd.encounter_num = of.encounter_num " +
            "WHERE of.provider_id = 'P21' " +
            "AND of.concept_cd LIKE 'ICD10GM%' " +
            "GROUP BY year " +
            "UNION ALL " +
            // OPS data
            "SELECT date_part('year', of.import_date) AS year, 'OPS' AS source, COUNT(DISTINCT of.encounter_num) AS count " +
            "FROM visit_dimension AS vd " +
            "JOIN observation_fact AS of ON vd.encounter_num = of.encounter_num " +
            "WHERE of.provider_id = 'P21' " +
            "AND of.concept_cd LIKE 'OPS%' " +
            "GROUP BY year " +
            ") AS combined_data " +
            "ORDER BY year, source";


    List<ImportStats> results = new ArrayList<>();

    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        int year = rs.getInt("year");
        CSVFile source = CSVFile.fromString(rs.getString("source"));
        int count = rs.getInt("count");

        results.add(new ImportStats(year, source, count));
      }
    }

    return results;
  }
}

