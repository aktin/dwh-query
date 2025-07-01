package org.aktin.generic.imports.manager.service;

import java.util.List;
import org.aktin.generic.imports.manager.model.ImportStats;
import org.aktin.generic.imports.manager.presenter.ImportStatsPresenter;
import org.aktin.generic.imports.manager.repository.ImportStatsRepository;

public class ImportStatsService {

  private final ImportStatsRepository repository;
  private final ImportStatsPresenter presenter;

  public ImportStatsService(ImportStatsRepository repository, ImportStatsPresenter presenter) {
    this.repository = repository;
    this.presenter = presenter;
  }

  public void displayStats() {
    try {
      List<ImportStats> stats = repository.getImportStats();
      // Delegate the output to the configured presenter
      presenter.present(stats);
    } catch (Exception e) {
      System.err.println("Error fetching import stats: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
