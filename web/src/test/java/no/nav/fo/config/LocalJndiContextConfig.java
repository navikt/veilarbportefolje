package no.nav.fo.config;

import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.ServiceUser;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LocalJndiContextConfig {
    private static int databaseCounter;
    private static final String HSQL_URL = "jdbc:hsqldb:mem:portefolje-" + databaseCounter++;

    public static SingleConnectionDataSource setupDataSourceWithCredentials(DbCredentials dbCredentials) {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setUrl(dbCredentials.getUrl());
        ds.setUsername(dbCredentials.getUsername());
        ds.setPassword(dbCredentials.getPassword());
        ds.setSuppressClose(true);
        if(dbCredentials.getUrl().contains("hsqldb")) {
            setHsqlToOraSyntax(ds);
        }
        if(!dbCredentials.getUrl().contains("oracle")) {
            // Migrere databaser så lenge vi ikke går mot en database i et felles miljø.
            // Antar inntil videre at dette kan oppnås ved å utelukke oracle-databaser.
            migrateDb(ds);
        }
        
        return ds;
    }

    public static SingleConnectionDataSource setupInMemoryDatabase() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setSuppressClose(true);
        ds.setUrl(HSQL_URL);
        ds.setUsername("sa");
        ds.setPassword("");

        setHsqlToOraSyntax(ds);
        migrateDb(ds);

        return ds;
    }

    private static void migrateDb(DriverManagerDataSource ds) {
        Flyway flyway = new Flyway();
        flyway.setSkipDefaultResolvers(true);
        flyway.setResolvers(new MergeMigrationResolver());
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
