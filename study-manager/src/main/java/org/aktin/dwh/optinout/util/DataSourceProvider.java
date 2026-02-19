package org.aktin.dwh.optinout.util;

import lombok.Getter;
import lombok.val;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

@Dependent
public class DataSourceProvider {
    private static final Logger log = Logger.getLogger(DataSourceProvider.class.getName());

    @Getter
    @Resource(lookup="java:jboss/datasources/QueryToolDemoDS")
    protected DataSource dataSource;

    /**
     * Method for testing. Resets the database to its initial state.
     * @throws SQLException  SQL error
     * @throws IOException  IO error
     */
    public void resetDatabaseEmpty() throws IOException, SQLException {
        try (val dbc = dataSource.getConnection()) {
            DatabaseTableManager dbm = new DatabaseTableManager(dbc);
            dbm.dropAllTables();
        }
    }

    public void initDatabase() throws IOException, SQLException {
        try (val dbc = this.dataSource.getConnection()) {
            DatabaseTableManager dbm = new DatabaseTableManager(dbc);
            dbm.checkAndCreateTables();
        }
    }

    @PostConstruct
    public void prepareDatabase() {
        log.info("Initializing study manager data source provider");
        try {
            initDatabase();
        } catch ( IOException | SQLException e) {
            throw new IllegalStateException("Unable to initialize study manager", e);
        }
    }
}
