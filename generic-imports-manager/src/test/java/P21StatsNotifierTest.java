import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.aktin.dwh.BrokerResourceManager;
import org.aktin.generic.imports.manager.P21StatsNotifier;
import org.aktin.generic.imports.manager.P21StatsSpec;
import org.aktin.generic.imports.manager.StatsQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class P21StatsNotifierTest {

  @Mock
  private BrokerResourceManager brokerResourceManager;

  @Mock
  private StatsQueryService statsQueryService;

  @InjectMocks
  private P21StatsNotifier p21StatsNotifier;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testUploadP21StatsToBroker() {
    Map<String, Object> row = new HashMap<>();
    row.put("year", "2025");
    row.put("count", 1000);
    row.put("source", "FAB");

    List<Map<String, Object>> results = Collections.singletonList(row);

    when(statsQueryService.run(org.mockito.Mockito.any(P21StatsSpec.class)))
        .thenReturn(results);

    p21StatsNotifier.uploadStatsInformation();

    Properties expectedProps = new Properties();
    expectedProps.put("FAB.2025", "1000");

    verify(brokerResourceManager).putMyResourceProperties(
        eq("p21stats"),
        org.mockito.ArgumentMatchers.argThat(props ->
            "1000".equals(props.get("FAB.2025")) && props.containsKey("timestamp"))
    );
  }
}
