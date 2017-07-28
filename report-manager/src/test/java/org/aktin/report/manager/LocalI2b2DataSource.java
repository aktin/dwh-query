package org.aktin.report.manager;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class LocalI2b2DataSource implements DataSource{

	private PrintWriter pw;
	private String defaultUser;
	private String defaultPassword;
	private String jdbcUri;
	
	public LocalI2b2DataSource(String jdbcUri, String defaultUser, String defaultPassword) {
		this.jdbcUri = jdbcUri;
		pw = new PrintWriter(System.out);
		this.defaultPassword = defaultPassword;
		this.defaultUser = defaultUser;
	}
	public LocalI2b2DataSource() {
		this("jdbc:postgresql://localhost:15432/i2b2", "i2b2crcdata", "");
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return pw;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		this.pw = out;
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(jdbcUri, defaultUser, defaultPassword);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return DriverManager.getConnection(jdbcUri,username, password);
	}

}
