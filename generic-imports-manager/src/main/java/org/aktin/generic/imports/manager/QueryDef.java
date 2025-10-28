package org.aktin.generic.imports.manager;

import java.util.List;

public final class QueryDef {

  public String name;
  public String sql;
  public List<Object> params;

  /**
   * Creates a new immutable definition of a single SQL query.
   * Each {@code QueryDef} corresponds to exactly one database query executed by {@link StatsQueryExecutor}.
   *
   * @param name   logical name of the query, used for identification in results
   * @param sql    SQL statement with parameter placeholders
   * @param params ordered list of parameter values for the SQL placeholders
   */
  public QueryDef(String name, String sql, List<Object> params) {
    this.name = name;
    this.sql = sql;
    this.params = params;
  }
}
