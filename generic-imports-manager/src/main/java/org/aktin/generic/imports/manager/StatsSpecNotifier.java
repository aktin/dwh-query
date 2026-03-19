package org.aktin.generic.imports.manager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.aktin.dwh.BrokerResourceManager;

/**
 * Uploads flattened statistics for a {@link StatsSpec}. Enforces at most one upload per spec id within 24 hours. Never throws; logs warnings on failure.
 */
@ApplicationScoped
public class StatsSpecNotifier {

  private static final Logger LOGGER = Logger.getLogger(StatsSpecNotifier.class.getName());

  @Inject
  private BrokerResourceManager brokerResourceManager;

  private final Map<String, Instant> lastUpload = new ConcurrentHashMap<>();

  /**
   * Attempts to upload the given properties for {@code specId}. Skips if an upload occurred within the last 24 hours.
   *
   * @param specId  specification identifier
   * @param results flattened results produced by {@link StatsSpec#toProperties(java.util.List)}
   */
  public void tryUpload(String specId, Properties results) {
    if (specId == null || results == null) {
      return;
    }
    final Instant now = Instant.now();
    final Instant cutoff = now.minus(1, ChronoUnit.DAYS);
    Instant prev = lastUpload.get(specId);
    if (prev != null && !prev.isBefore(cutoff)) {
      return; // already uploaded within 24h
    }
    results.put("timestamp", now.toString());
    try {
      brokerResourceManager.putMyResourceProperties(specId, results);
      lastUpload.put(specId, now);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Stats upload failed for spec: " + specId, e);
    }
  }
}
