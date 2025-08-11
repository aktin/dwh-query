package org.aktin.generic.imports.manager;

import java.util.List;

public interface StatsSpec {

  String id();

  List<QueryDef> queries();
}
