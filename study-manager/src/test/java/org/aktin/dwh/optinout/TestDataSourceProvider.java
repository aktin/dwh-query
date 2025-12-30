package org.aktin.dwh.optinout;

import javax.sql.DataSource;

public class TestDataSourceProvider extends DataSourceProvider {
    TestDataSourceProvider(DataSource ds) {
        this.dataSource = ds;
    }
}
