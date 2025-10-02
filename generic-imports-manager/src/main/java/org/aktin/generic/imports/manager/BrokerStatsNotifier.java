package org.aktin.generic.imports.manager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.aktin.dwh.BrokerResourceManager;

@ApplicationScoped
public class BrokerStatsNotifier {

  @Inject
  private BrokerResourceManager brokerResourceManager;

  @Inject
  private StatsQueryService statsQueryService;

  private final Map<String, Instant> lastStats = new HashMap<>();

  public void upload(StatsSpec spec) {
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

    List<Map<String, Object>> results = statsQueryService.run(spec);
    Properties props = flattenResults(results);
    props.put("timestamp", Instant.now().toString());

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
