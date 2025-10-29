import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.aktin.generic.imports.manager.P21StatsSpec;
import org.aktin.generic.imports.manager.QueryDef;
import org.aktin.generic.imports.manager.QueryResult;
import org.aktin.generic.imports.manager.StatsQueryExecutor;
import org.aktin.generic.imports.manager.StatsQueryService;
import org.aktin.generic.imports.manager.StatsSpec;
import org.aktin.generic.imports.manager.StatsSpecNotifier;
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
    StatsSpecNotifier notifier = new StatsSpecNotifier() {
      @Override
      public void tryUpload(String id, Properties props) {
        // do nothing
      }
    };
    service = new StatsQueryService(executor, notifier);
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

      // Other stuff
      stmt.execute("INSERT INTO observation_fact VALUES (7, '@', 'P21:DEP999', '2025-05-01')");
      stmt.execute("INSERT INTO observation_fact VALUES (8, 'P21', 'OTHER:STUFF', '2025-05-01')");
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
    Properties props = service.run(spec);
    assertFalse(props.isEmpty(), "Expected non-empty results");
  }

  @Test
  void testEmptyResult() throws Exception {
    try (Connection c = dataSource.getConnection()) {
      emptyTestData(c);
    }
    Properties props = service.run(new P21StatsSpec());
    assertTrue(props.values().stream().allMatch(v -> "0".equals(v.toString())), "Expected all stats to be zero when tables are empty");
    try (Connection c = dataSource.getConnection()) {
      insertTestData(c);
    }
  }

  @Test
  void testNonexistingTable() {
    StatsSpec badSpec = new StatsSpec() {
      public String id() {
        return "bad";
      }

      public List<QueryDef> queries() {
        return Collections.singletonList(new QueryDef("BAD", "SELECT * FROM table_that_does_not_exist", Collections.emptyList()));
      }

      public Properties toProperties(List<QueryResult> results) {
        return new Properties();
      }
    };

    Properties result = service.run(badSpec);
    assertTrue(result.isEmpty(), "Expected empty result on bad SQL");
  }

  @Test
  void testSqlTimeout() {
    StatsQueryExecutor executor = new StatsQueryExecutor(dataSource, 1);
    StatsSpecNotifier notifier = new StatsSpecNotifier();
    StatsQueryService serviceLowTimeout = new StatsQueryService(executor, notifier);

    StatsSpec slowSpec = new StatsSpec() {
      public String id() {
        return "slow";
      }

      public List<QueryDef> queries() {
        return Collections.singletonList(new QueryDef("SLOW", "SELECT pg_sleep(5)", Collections.emptyList()));
      }

      public Properties toProperties(List<QueryResult> results) {
        return new Properties();
      }
    };

    Properties result = serviceLowTimeout.run(slowSpec);
    assertTrue(result.isEmpty(), "Expected empty result on timeout");
  }
}
