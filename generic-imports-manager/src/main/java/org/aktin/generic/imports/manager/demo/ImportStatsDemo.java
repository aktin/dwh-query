package org.aktin.generic.imports.manager.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.aktin.generic.imports.manager.ImportStats;
import org.aktin.generic.imports.manager.P21ImportStatsQueryManager;

public class ImportStatsDemo {

  public static void main(String[] args) throws Exception {
    Connection conn = DriverManager.getConnection(
        "jdbc:postgresql://localhost:5432/i2b2?searchPath=i2b2crcdata",
        "i2b2crcdata", "demouser"
    );

    P21ImportStatsQueryManager p21ImportStatsQueryManager = new P21ImportStatsQueryManager(conn);
    List<ImportStats> stats = p21ImportStatsQueryManager.fetchFabStats();
    System.out.println(stats);
  }
}
