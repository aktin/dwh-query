package org.aktin.generic.imports.manager;

import java.util.List;
import java.util.Map;

public final class QueryResult {

  private String name;
  private List<Map<String, Object>> rows;
  private List<String> columns;

  /**
   * Creates a new container for the complete result of a single executed SQL query.
   *
   * @param name    logical name of the originating query
   * @param rows    result rows as maps of column label to value
   * @param columns ordered list of column labels
   */
  public QueryResult(String name, List<Map<String, Object>> rows, List<String> columns) {
    this.name = name;
    this.rows = rows;
    this.columns = columns;
  }

  public String getName() {
    return name;
  }

  public List<Map<String, Object>> getRows() {
    return rows;
  }

  public List<String> getColumns() {
    return columns;
  }
}
