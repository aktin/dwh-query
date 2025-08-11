package org.aktin.generic.imports.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class P21StatsSpec implements StatsSpec {

  public String id() {
    return "p21";
  }

  public List<QueryDef> queries() {
    String sql = "SELECT date_part('year', of.import_date) AS year, "
        + "COUNT(DISTINCT of.encounter_num) AS count "
        + "FROM visit_dimension vd "
        + "JOIN observation_fact of ON vd.encounter_num = of.encounter_num "
        + "WHERE of.provider_id = 'P21' AND of.concept_cd LIKE ? "
        + "GROUP BY year";

    List<QueryDef> list = new ArrayList<>();
    list.add(new QueryDef("FAB", sql, Collections.singletonList("P21:DEP%")));
    list.add(new QueryDef("FALL", sql, Collections.singletonList("P21:ADMC%")));
    list.add(new QueryDef("ICD", sql, Collections.singletonList("ICD10GM%")));
    list.add(new QueryDef("OPS", sql, Collections.singletonList("OPS%")));
    return list;
  }
}
