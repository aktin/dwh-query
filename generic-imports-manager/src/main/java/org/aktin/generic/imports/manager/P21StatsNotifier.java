package org.aktin.generic.imports.manager;

import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class P21StatsNotifier {

  private static final Logger LOGGER = Logger.getLogger(P21StatsNotifier.class.getName());

  @Inject
  private BrokerStatsNotifier brokerStatsNotifier;

  public void onStatsViewed(@Observes StatsViewedEvent event) {
    StatsSpec spec = event.getSpec();
    if ("p21".equals(spec.id())) {
      LOGGER.info("P21StatsNotifier observed p21 view -> requesting broker upload");
      brokerStatsNotifier.upload(event.getSpec(), event.getResults());
    }
  }
}
