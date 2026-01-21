package org.aktin.dwh.optinout.util;

import lombok.Getter;
import lombok.val;
import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

@Dependent
public class DataSourceProvider {
    private static final Logger log = Logger.getLogger(DataSourceProvider.class.getName());

    @Inject
    private Preferences prefs;
    @Getter
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
            initDataSource();
            initDatabase();
        } catch ( IOException | SQLException | NamingException e) {
            throw new IllegalStateException("Unable to initialize study manager", e);
        }
    }

    /**
     * Datasource JNDI lookup name is not known during compile time (for @Resource) and needs to be retrieved from preferences
     * @throws NamingException
     */
    public void initDataSource() throws NamingException {
        if( dataSource == null ) {
            val dsName = prefs.get(PreferenceKey.i2b2DatasourceCRC);
            log.info("Using i2b2 database via "+dsName);
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource)ctx.lookup(dsName);
        }
    }
}
