package org.aktin.generic.imports.manager;

import java.util.List;
import java.util.Map;

public class StatsViewedEvent {
  private final StatsSpec spec;
  private final List<Map<String, Object>> results;

  public StatsViewedEvent(StatsSpec spec, List<Map<String, Object>> results) {
    this.spec = spec;
    this.results = results;
  }

  public StatsSpec getSpec() {
    return spec;
  }

  public List<Map<String, Object>> getResults() {
    return results;
  }
}
