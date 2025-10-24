package org.aktin.generic.imports.manager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;
import org.aktin.dwh.BrokerResourceManager;

/**
 * Uploader for statistics results of {@link StatsSpec}. Uploads at most once per 24h per {@link StatsSpec#id()}. Never throws; logs warnings on failure.
 */
@ApplicationScoped
public class StatsSpecNotifier {

  private static final Logger LOGGER = Logger.getLogger(StatsSpecNotifier.class.getName());

  @Inject
  private BrokerResourceManager brokerResourceManager;

  private final Map<String, Instant> lastUpload = new ConcurrentHashMap<>();

  /**
   * Try to upload results for the given spec id. Skips if uploaded within the last 24h.
   *
   * @param specId  spec identifier
   * @param results executor results (row maps)
   */
  public void tryUpload(String specId, List<Map<String, Object>> results) {
    if (specId == null || results == null) {
      return;
    }
    final Instant now = Instant.now();
    final Instant cutoff = now.minus(1, ChronoUnit.DAYS);

    Instant prev = lastUpload.get(specId);
    if (prev != null && !prev.isBefore(cutoff)) {
      return; // already uploaded within 24h
    }
    Properties props = flattenResults(results);
    props.put("timestamp", now.toString());
    try {
      brokerResourceManager.putMyResourceProperties(specId, props);
      lastUpload.put(specId, now);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Stats upload failed for spec: " + specId, e);
    }
  }

  /**
   * Builds properties as source.year=count; ignores incomplete rows.
   */
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
