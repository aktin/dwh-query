package org.aktin.generic.imports.manager;

import java.util.List;
import java.util.Properties;

/**
 * Groups one or more SQL queries into a named statistics specification. Used by {@link StatsQueryExecutor} to collect metrics.
 */
public interface StatsSpec {

  /**
   * Returns the unique identifier of this statistics specification.
   */
  String id();

  /**
   * Returns all queries belonging to this specification.
   */
  List<QueryDef> queries();

  /**
   * Converts the raw query results into a flattened {@link Properties} object suitable for broker upload or serialization.
   */
  Properties toProperties(List<QueryResult> results);
}
