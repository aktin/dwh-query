import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.aktin.generic.imports.manager.P21ImportStats;
import org.aktin.generic.imports.manager.P21StatsQueryManagerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class P21StatsQueryManagerServiceTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  private static Connection connection;

  @BeforeAll
  static void setUp() throws Exception {
    connection = createConnection();
    createTables(connection);
    insertTestData(connection);
  }

  private static Connection createConnection() throws SQLException {
    return DriverManager.getConnection(
        postgres.getJdbcUrl(),
        postgres.getUsername(),
        postgres.getPassword()
    );
  }

  private static void createTables(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE TABLE visit_dimension (encounter_num INT)");
      stmt.execute("CREATE TABLE observation_fact (" +
          "encounter_num INT, " +
          "provider_id VARCHAR(50), " +
          "concept_cd VARCHAR(50), " +
          "import_date DATE)");
    }
  }

  private static void insertTestData(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("INSERT INTO visit_dimension VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9)");

      // FALL - one encounter
      stmt.execute("INSERT INTO observation_fact VALUES (3, 'P21', 'P21:ADMC001', '2024-02-20')");

      // FAB - two encounters, same year
      stmt.execute("INSERT INTO observation_fact VALUES (1, 'P21', 'P21:DEP001', '2025-01-10')");
      stmt.execute("INSERT INTO observation_fact VALUES (2, 'P21', 'P21:DEP002', '2025-01-15')");

      // ICD - two encounters, different years
      stmt.execute("INSERT INTO observation_fact VALUES (4, 'P21', 'ICD10GM:ABC', '2025-03-25')");
      stmt.execute("INSERT INTO observation_fact VALUES (5, 'P21', 'ICD10GM:DEF', '2024-03-25')");

      // OPS - duplicate encounter, should be counted once
      stmt.execute("INSERT INTO observation_fact VALUES (6, 'P21', 'OPS:XYZ', '2025-04-05')");
      stmt.execute("INSERT INTO observation_fact VALUES (6, 'P21', 'OPS:XYZ', '2025-04-05')");

      // Noise data
      stmt.execute("INSERT INTO observation_fact VALUES (7, '@', 'P21:DEP999', '2025-05-01')"); // wrong provider
      stmt.execute("INSERT INTO observation_fact VALUES (8, 'P21', 'OTHER:STUFF', '2025-05-01')"); // wrong concept
    }
  }


  @AfterAll
  static void tearDown() throws Exception {
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

  @Test
  void testFetchP21Stats() throws SQLException {
    P21StatsQueryManagerService manager = new P21StatsQueryManagerService(connection);
    List<P21ImportStats> stats = manager.fetchAllP21Stats();
    assertFallStats(stats);
    assertFabStats(stats);
    assertIcdStats(stats);
    assertOpsStats(stats);
  }

  private void assertFallStats(List<P21ImportStats> stats) {
    Map<Integer, Integer> yearToCount = getCounts(stats, "FALL");
    assertEquals(1, yearToCount.size());
    assertEquals(1, yearToCount.get(2024));
  }

  private void assertFabStats(List<P21ImportStats> stats) {
    Map<Integer, Integer> yearToCount = getCounts(stats, "FAB");
    assertEquals(1, yearToCount.size());
    assertEquals(2, yearToCount.get(2025));
  }

  private void assertIcdStats(List<P21ImportStats> stats) {
    Map<Integer, Integer> yearToCount = getCounts(stats, "ICD");
    assertEquals(2, yearToCount.size());
    assertEquals(1, yearToCount.get(2024));
    assertEquals(1, yearToCount.get(2025));
  }

  private void assertOpsStats(List<P21ImportStats> stats) {
    Map<Integer, Integer> yearToCount = getCounts(stats, "OPS");
    assertEquals(1, yearToCount.size());
    assertEquals(1, yearToCount.get(2025));
  }

  private Map<Integer, Integer> getCounts(List<P21ImportStats> stats, String source) {
    return stats.stream().filter(s -> s.getSource().equals(source))
        .collect(Collectors.toMap(P21ImportStats::getYear, P21ImportStats::getCount));
  }
}
