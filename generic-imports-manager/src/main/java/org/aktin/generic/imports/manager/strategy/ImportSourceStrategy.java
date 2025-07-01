package org.aktin.generic.imports.manager.strategy;

import java.sql.Connection;
import java.util.List;
import org.aktin.generic.imports.manager.model.ImportStats;

public interface ImportSourceStrategy {

  String getSourceName();

  List<ImportStats> fetchStats(Connection connection) throws Exception;
}
