package org.aktin.generic.imports.manager;

import java.util.List;

/**
 * Defines a set of SQL queries to collect statistics for a specific data import type. Implementations provide an identifier and the queries to be executed.
 */
public interface StatsSpec {

  /**
   * Returns the unique identifier for this statistics specification.
   *
   * @return specification ID
   */
  String id();

  /**
   * Returns the list of queries to execute for this specification.
   *
   * @return list of query definitions
   */
  List<QueryDef> queries();
}
