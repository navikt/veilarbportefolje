package no.nav.fo.config;

import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.fo.database.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LocalJndiContextConfig {

    public static SingleConnectionDataSource setupDataSourceWithCredentials(DbCredentials dbCredentials) {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setUrl(dbCredentials.getUrl());
        ds.setUsername(dbCredentials.getUsername());
        ds.setPassword(dbCredentials.getPassword());
        ds.setSuppressClose(true);
        if(dbCredentials.getUrl().contains("hsqldb")) {
            setHsqlToOraSyntax(ds);
        }
        return ds;
    }

    public static SingleConnectionDataSource setupInMemoryDatabase() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setSuppressClose(true);
        ds.setDriverClassName(TestDriver.class.getName());
        ds.setUrl(TestDriver.URL);
        ds.setUsername("sa");
        ds.setPassword("");

        setHsqlToOraSyntax(ds);
        migrateDb(ds);

        return ds;
    }

    public static void migrateDb(DriverManagerDataSource ds) {
        Flyway flyway = new Flyway();
        flyway.setLocations("testmigration");
        flyway.setDataSource(ds);
        flyway.migrate();
    }

    private static void setHsqlToOraSyntax(SingleConnectionDataSource ds) {
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SET DATABASE SQL SYNTAX ORA TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setServiceUserCredentials(ServiceUser serviceUser) {
        System.setProperty("no.nav.modig.security.systemuser.username", serviceUser.getUsername());
        System.setProperty("no.nav.modig.security.systemuser.password", serviceUser.getPassword());
    }

}
