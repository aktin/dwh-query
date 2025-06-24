package org.aktin.generic.imports.manager.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.aktin.generic.imports.manager.model.ImportStats;
import org.aktin.generic.imports.manager.repository.ImportStatsRepository;

public class ImportStatsService {

  private final ImportStatsRepository repository;

  public ImportStatsService(ImportStatsRepository repository) {
    this.repository = repository;
  }

  public void displayStats() {
    try {
      List<ImportStats> stats = repository.getImportStats();
      if (stats.isEmpty()) {
        System.out.println("No import data found.");
        return;
      }

      int currentYear = -1;
      for (ImportStats stat : stats) {
        if (stat.getYear() != currentYear) {
          currentYear = stat.getYear();
          System.out.println("Year: " + currentYear);
        }
        System.out.println("  " + stat.getSource() + ": " + stat.getCount());
      }

    } catch (Exception e) {
      System.err.println("Error fetching import stats:");
      e.printStackTrace();
    }
  }
}
