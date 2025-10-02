package org.aktin.generic.imports.manager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.aktin.dwh.BrokerResourceManager;

@ApplicationScoped
public class BrokerStatsNotifier {

  @Inject
  private BrokerResourceManager brokerResourceManager;

  @Inject
  private StatsQueryService statsQueryService;

  private Map<String, Instant> lastStats;

  @PostConstruct
  public void init() {
    this.lastStats = new HashMap<>();
  }

  public void upload(StatsSpec spec, List<Map<String, Object>> results) {
    Instant now = Instant.now();
    Instant cutoff = now.minus(1, ChronoUnit.DAYS);

    boolean shouldUpload = lastStats.compute(spec.id(), (k, prev) -> {
      if (prev == null || prev.isBefore(cutoff)) {
        return now;
      } else {
        return prev;
      }
    }).equals(now);

    if (!shouldUpload) {
      return;
    }

    Properties props = flattenResults(results);
    props.put("timestamp", now.toString());

    brokerResourceManager.putMyResourceProperties(spec.id(), props);
  }

  private Properties flattenResults(List<Map<String, Object>> results) {
    Properties props = new Properties();
    for (Map<String, Object> row : results) {
      Object year = row.get("year");
      Object count = row.get("count");
      Object source = row.get("source");
      if (year != null && count != null && source != null) {
        props.put(source + "." + year, String.valueOf(count));
      }
    }
    return props;
  }
}
