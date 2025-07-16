import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.aktin.generic.imports.manager.P21ImportStats;
import org.aktin.generic.imports.manager.P21StatsQueryManagerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class P21StatsQueryManagerServiceTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  private static Connection connection;

  @BeforeAll
  static void setUp() throws Exception {
    connection = DriverManager.getConnection(
        postgres.getJdbcUrl(),
        postgres.getUsername(),
        postgres.getPassword()
    );

    try (Statement stmt = connection.createStatement()) {
      stmt.execute("CREATE TABLE visit_dimension (encounter_num INT)");
      stmt.execute("CREATE TABLE observation_fact (" +
          "encounter_num INT, " +
          "provider_id VARCHAR(50), " +
          "concept_cd VARCHAR(50), " +
          "import_date DATE)");

      stmt.execute("INSERT INTO visit_dimension VALUES (1), (2), (3), (4)");
      stmt.execute("INSERT INTO observation_fact VALUES (1, 'P21', 'P21:DEP001', '2025-07-16')");
      stmt.execute("INSERT INTO observation_fact VALUES (2, 'P21', 'ICD10GM:ABC', '2025-07-16')");
      stmt.execute("INSERT INTO observation_fact VALUES (3, 'P21', 'OPS:XYZ', '2025-07-16')");
      stmt.execute("INSERT INTO observation_fact VALUES (4, 'P21', 'P21:ADMC:R', '2025-07-16')");
    }
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

  @Test
  void testFetchFabStats() throws Exception {
    P21StatsQueryManagerService manager = new P21StatsQueryManagerService(connection);

    List<P21ImportStats> stats = manager.getStatsBySource("FAB");

    assertFalse(stats.isEmpty());
    assertEquals("FAB", stats.get(0).getSource());
  }

  @Test
  void TestFetchFallStats() throws Exception {
    P21StatsQueryManagerService manager = new P21StatsQueryManagerService(connection);

    List<P21ImportStats> stats = manager.getStatsBySource("FALL");
    stats.forEach(System.out::println);

    assertFalse(stats.isEmpty());
    assertEquals("FALL", stats.get(0).getSource());
  }

  @Test
  void TestFetchIcdStats() throws Exception {
    P21StatsQueryManagerService manager = new P21StatsQueryManagerService(connection);

    List<P21ImportStats> stats = manager.getStatsBySource("ICD");

    assertFalse(stats.isEmpty());
    assertEquals("ICD", stats.get(0).getSource());
  }

  @Test
  void TestFetchOpsStats() throws Exception {
    P21StatsQueryManagerService manager = new P21StatsQueryManagerService(connection);

    List<P21ImportStats> stats = manager.getStatsBySource("OPS");

    assertFalse(stats.isEmpty());
    assertEquals("OPS", stats.get(0).getSource());
  }
}
