package org.aktin.generic.imports.manager;

import java.util.List;

/**
 * Groups one or more SQL queries into a named statistics specification. Used by {@link StatsQueryExecutor} to collect metrics.
 */
public interface StatsSpec {

  /**
   * Unique id of this statistics specification.
   */
  String id();

  /**
   * All queries belonging to this specification.
   */
  List<QueryDef> queries();
}
