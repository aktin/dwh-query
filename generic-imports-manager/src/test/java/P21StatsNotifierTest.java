import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.aktin.generic.imports.manager.BrokerStatsNotifier;
import org.aktin.generic.imports.manager.P21StatsNotifier;
import org.aktin.generic.imports.manager.P21StatsSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class P21StatsNotifierTest {

  @Mock
  private BrokerStatsNotifier brokerStatsNotifier;

  @InjectMocks
  private P21StatsNotifier p21StatsNotifier;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testDelegatesToBrokerStatsNotifier() {
    p21StatsNotifier.uploadStatsInformation();

    verify(brokerStatsNotifier).upload(eq("stats/p21"), any(P21StatsSpec.class));
  }
}
