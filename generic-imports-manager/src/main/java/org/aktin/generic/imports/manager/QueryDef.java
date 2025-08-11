package org.aktin.generic.imports.manager;

import java.util.List;

public class QueryDef {

  private final String name;
  private final String sql;
  private final List<Object> params;

  public QueryDef(String name, String sql, List<Object> params) {
    this.name = name;
    this.sql = sql;
    this.params = params;
  }

  public String getName() {
    return name;
  }

  public String getSql() {
    return sql;
  }

  public List<Object> getParams() {
    return params;
  }
}
