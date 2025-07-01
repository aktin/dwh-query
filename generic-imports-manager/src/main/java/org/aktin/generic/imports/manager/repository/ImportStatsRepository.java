package org.aktin.generic.imports.manager.repository;

import java.util.List;
import org.aktin.generic.imports.manager.model.ImportStats;

public interface ImportStatsRepository {

  List<ImportStats> getImportStats() throws Exception;
}
