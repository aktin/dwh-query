package org.aktin.generic.imports.manager.presenter;

import java.util.List;
import org.aktin.generic.imports.manager.model.ImportStats;

public class JsonImportStatsPresenter implements  ImportStatsPresenter {

  @Override
  public void present(List<ImportStats> stats) {
    if (stats.isEmpty()) {
      return;
    }
  }
}
