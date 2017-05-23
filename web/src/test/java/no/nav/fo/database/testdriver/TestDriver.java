package no.nav.fo.database.testdriver;


import no.nav.fo.util.ProxyUtils;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static java.sql.DriverManager.registerDriver;

public class TestDriver implements Driver {

    private static int databaseCounter;

    public static final String URL = TestDriver.class.getSimpleName();
    private static final String HSQL_URL = "jdbc:hsqldb:mem:portefolje-" + databaseCounter++;

    static {
        try {
            registerDriver(new TestDriver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Driver driver = new org.hsqldb.jdbcDriver();

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        return ProxyUtils.proxy(new ConnectionInvocationHandler(driver.connect(getHsqlUrl(), info)), Connection.class);
    }

    private String getHsqlUrl() {
        return HSQL_URL;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(HSQL_URL, info);
    }

    @Override
    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driver.getParentLogger();
    }

}
