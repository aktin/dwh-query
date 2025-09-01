import org.aktin.generic.imports.manager.P21StatsNotifier;
import org.junit.Test;

public class P21StatsNotifierTest {
  @Test
  public void testP21StatsNotifier() {
    P21StatsNotifier notifier = new P21StatsNotifier();
    notifier.uploadP21ImportStatus();
  }
}
