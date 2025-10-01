package org.aktin.generic.imports.manager;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;

@Singleton
public class P21StatsNotifier {

  @Inject
  private BrokerStatsNotifier brokerStatsNotifier;

  public P21StatsNotifier() {}

  // Used only in tests
  public P21StatsNotifier(BrokerStatsNotifier brokerStatsNotifier) {
    this.brokerStatsNotifier = brokerStatsNotifier;
  }

  @PostConstruct
  public void uploadStatsInformation() {
    brokerStatsNotifier.upload(new P21StatsSpec());
  }
}
