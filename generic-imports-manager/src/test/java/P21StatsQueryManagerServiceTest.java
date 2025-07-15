import org.aktin.generic.imports.manager.P21ImportStats;
import org.aktin.generic.imports.manager.P21StatsQueryManagerService;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class P21StatsQueryManagerServiceTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  private Connection connection;

  @BeforeEach
  void setUp() throws Exception {
    // Verbindung zur Container-Datenbank
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

      stmt.execute("INSERT INTO visit_dimension VALUES (1), (2), (3)");
      stmt.execute("INSERT INTO observation_fact VALUES (1, 'P21', 'P21:DEP001', '2024-01-01')");
      stmt.execute("INSERT INTO observation_fact VALUES (2, 'P21', 'ICD10GM:ABC', '2024-02-01')");
      stmt.execute("INSERT INTO observation_fact VALUES (3, 'P21', 'OPS:XYZ', '2024-03-01')");
    }
  }

  @AfterEach
  void tearDown() throws Exception {
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
}
