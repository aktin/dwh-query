package org.aktin.generic.imports.manager;

public interface StatsNotifier {
  /**
   * Returns the unique identifier for this statistics specification.
   *
   * @return specification ID
   */
  String id();

  void uploadStatsInformation();
}
