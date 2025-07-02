package org.aktin.generic.imports.manager;

import java.sql.SQLException;
import java.util.List;

// TODO: gute Name
public interface GenericQueryManager<T> {

  String buildQuery(String query);

  List<T> executeQuery(String sql, String sourceName) throws SQLException;
}