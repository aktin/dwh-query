import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.aktin.generic.imports.manager.P21StatsSpec;
import org.aktin.generic.imports.manager.QueryDef;
import org.aktin.generic.imports.manager.StatsQueryExecutor;
import org.aktin.generic.imports.manager.StatsQueryService;
import org.aktin.generic.imports.manager.StatsSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class P21StatsSpecTest {

  private static DataSource dataSource;

  private static StatsQueryService service;

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @BeforeAll
  static void setUp() throws Exception {
    dataSource = buildDataSourceFromContainer(postgres);
    try (Connection c = dataSource.getConnection()) {
      createTables(c);
      insertTestData(c);
    }
    StatsQueryExecutor executor = new StatsQueryExecutor(dataSource, 60);
    service = new StatsQueryService(executor);
  }

  private static DataSource buildDataSourceFromContainer(PostgreSQLContainer<?> pg) {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setURL(pg.getJdbcUrl());
    ds.setUser(pg.getUsername());
    ds.setPassword(pg.getPassword());
    return ds;
  }

  private static void createTables(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE TABLE visit_dimension (encounter_num INT)");
      stmt.execute("CREATE TABLE observation_fact (" +
          "encounter_num INT, " +
          "provider_id VARCHAR(50), " +
          "concept_cd VARCHAR(50), " +
          "start_date DATE)");
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

  private static void emptyTestData(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.execute("TRUNCATE observation_fact");
      stmt.execute("TRUNCATE visit_dimension");
    }
  }

  @Test
  void testFetchP21Stats() {
    P21StatsSpec spec = new P21StatsSpec();
    List<Map<String, Object>> stats = service.run(spec);
    assertFallStats(stats);
    assertFabStats(stats);
    assertIcdStats(stats);
    assertOpsStats(stats);
  }

  private void assertFallStats(List<Map<String, Object>> stats) {
    Map<Integer, Integer> yearToCount = getCounts(stats, "FALL");
    assertEquals(1, yearToCount.size());
    assertEquals(1, yearToCount.get(2024));
  }

  private void assertFabStats(List<Map<String, Object>> stats) {
    Map<Integer, Integer> yearToCount = getCounts(stats, "FAB");
    assertEquals(1, yearToCount.size());
    assertEquals(2, yearToCount.get(2025));
  }

  private void assertIcdStats(List<Map<String, Object>> stats) {
    Map<Integer, Integer> yearToCount = getCounts(stats, "ICD");
    assertEquals(2, yearToCount.size());
    assertEquals(1, yearToCount.get(2024));
    assertEquals(1, yearToCount.get(2025));
  }

  private void assertOpsStats(List<Map<String, Object>> stats) {
    Map<Integer, Integer> yearToCount = getCounts(stats, "OPS");
    assertEquals(1, yearToCount.size());
    assertEquals(1, yearToCount.get(2025));
  }

  private Map<Integer, Integer> getCounts(List<Map<String, Object>> stats, String source) {
    return stats.stream()
        .filter(s -> source.equals(s.get("source")))
        .collect(Collectors.toMap(
            s -> ((Number) s.get("year")).intValue(),
            s -> ((Number) s.get("count")).intValue()
        ));
  }

  @Test
  void testEmptyResult() throws Exception {
    try (Connection c = dataSource.getConnection()) {
      emptyTestData(c);
    }
    List<Map<String, Object>> stats = service.run(new P21StatsSpec());
    assertTrue(stats.isEmpty());
    try (Connection c = dataSource.getConnection()) {
      insertTestData(c);
    }
  }

  @Test
  void testNonexistingTable() throws Exception {
    StatsSpec badSpec = new StatsSpec() {
      public String id() {
        return "bad";
      }

      public List<QueryDef> queries() {
        return Collections.singletonList(
            new QueryDef("BAD", "SELECT * FROM table_that_does_not_exist", Collections.emptyList())
        );
      }
    };
    List<Map<String, Object>> result = service.run(badSpec);
    assertEquals(new ArrayList<>(), result);
  }

  @Test
  void testSqlTimeout() {
    StatsQueryExecutor executor = new StatsQueryExecutor(dataSource, 1);
    StatsQueryService serviceLowTimeout = new StatsQueryService(executor);
    StatsSpec slowSpec = new StatsSpec() {
      public String id() {
        return "slow";
      }

      public java.util.List<QueryDef> queries() {
        return java.util.Collections.singletonList(new QueryDef("SLOW", "SELECT pg_sleep(5)", java.util.Collections.emptyList()));
      }
    };
    List<Map<String, Object>> result = serviceLowTimeout.run(slowSpec);
    assertEquals(new ArrayList<>(), result);
  }
}
