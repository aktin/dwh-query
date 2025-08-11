package org.aktin.generic.imports.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class StatsQueryService {

  private final StatsQueryExecutor executor;

  @Inject
  public StatsQueryService(StatsQueryExecutor executor) {
    this.executor = executor;
  }

  public List<Map<String, Object>> run(StatsSpec spec) throws SQLException {
    List<Map<String, Object>> out = new ArrayList<>();
    for (QueryDef q : spec.queries()) {
      out.addAll(executor.run(q));
    }
    return out;
  }
}
