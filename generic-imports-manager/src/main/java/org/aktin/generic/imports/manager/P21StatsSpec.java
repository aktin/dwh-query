package org.aktin.generic.imports.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Statistics specification for P21 data. Defines all SQL queries needed to count encounters per year and category. Queried sources: FAB, FALL, ICD, OPS.
 */
public class P21StatsSpec implements StatsSpec {

  /**
   * {@inheritDoc}
   */
  public String id() {
    return "p21";
  }

  /**
   * {@inheritDoc}
   */
  public List<QueryDef> queries() {
    String sql = "SELECT date_part('year', of.start_date) AS year, "
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Properties toProperties(List<QueryResult> results) {
    Properties p = new Properties();
    for (QueryResult r : results) {
      final String source = r.getName(); // FAB/FALL/ICD/OPS
      for (Map<String, Object> row : r.getRows()) {
        Object year = row.get("year");
        Object count = row.get("count");
        if (year != null && count != null) {
          p.put(source + "." + year, String.valueOf(count));
        }
      }
    }
    return p;
  }
}
