package org.aktin.generic.imports.manager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.aktin.dwh.BrokerResourceManager;


@Singleton
@Startup
public class P21StatsNotifier  implements StatsNotifier {

  @Inject
  private BrokerResourceManager brokerResourceManager;

  @Inject
  private StatsQueryService statsQueryService;

  private final P21StatsSpec p21StatsSpec;

  public P21StatsNotifier() {
    this.p21StatsSpec = new P21StatsSpec();
  }

  public String id() {
    return "p21stats";
  }

  @PostConstruct
  public void uploadStatsInformation() {
    List<Map<String, Object>> results = statsQueryService.run(p21StatsSpec);
    System.out.println(results);
    Properties props = flattenResults(results);
    props.put("timestamp", Instant.now());
    brokerResourceManager.putMyResourceProperties(this.id(), props);
  }

  private Properties flattenResults(List<Map<String, Object>> results) {
    Properties props = new Properties();
    for (Map<String, Object> row : results) {
      Object year = row.get("year");
      Object count =  row.get("count");
      Object source =  row.get("source");
      if (year != null && count != null && source != null) {
        props.put(source + "." + year, String.valueOf(count));
      }
    }
    return props;
  }
}
