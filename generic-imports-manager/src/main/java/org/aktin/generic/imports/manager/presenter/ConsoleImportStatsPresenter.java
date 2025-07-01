package org.aktin.generic.imports.manager.presenter;

import java.util.List;
import org.aktin.generic.imports.manager.model.ImportStats;

public class ConsoleImportStatsPresenter implements ImportStatsPresenter {

  @Override
  public void present(List<ImportStats> stats) {
    if (stats.isEmpty()) {
      System.out.println("No import data found");
      return;
    }
    int currentYear = -1;
    for (ImportStats stat : stats) {
      if (stat.getYear() != currentYear) {
        currentYear = stat.getYear();
        System.out.println("Current year: " + currentYear);
      }
      System.out.println("  " + stat.getSource() + ": " + stat.getCount());
    }
  }
}
