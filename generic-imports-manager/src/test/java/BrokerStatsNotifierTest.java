import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.aktin.dwh.BrokerResourceManager;
import org.aktin.generic.imports.manager.BrokerStatsNotifier;
import org.aktin.generic.imports.manager.P21StatsSpec;
import org.aktin.generic.imports.manager.StatsQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BrokerStatsNotifierTest {

  @Mock
  private BrokerResourceManager brokerResourceManager;

  @Mock
  private StatsQueryService statsQueryService;

  @InjectMocks
  private BrokerStatsNotifier brokerStatsNotifier;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testUploadFlattensAndSendsProps() {
    Map<String, Object> row = new HashMap<>();
    row.put("year", "2025");
    row.put("count", 1000);
    row.put("source", "FAB");
    List<Map<String, Object>> results = Collections.singletonList(row);

    when(statsQueryService.run(org.mockito.ArgumentMatchers.any(P21StatsSpec.class)))
        .thenReturn(results);

    brokerStatsNotifier.upload("stats/p21", new P21StatsSpec());

    verify(brokerResourceManager).putMyResourceProperties(
        eq("stats/p21"),
        argThat((Properties props) ->
            "1000".equals(props.get("FAB.2025")) && props.get("timestamp") != null
        )
    );
  }
}
