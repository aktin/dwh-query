package org.aktin.dwh.optinout;

import org.aktin.dwh.optinout.util.DataSourceProvider;

import javax.sql.DataSource;

public class TestDataSourceProvider extends DataSourceProvider {
    TestDataSourceProvider(DataSource ds) {
        this.dataSource = ds;
    }
}
