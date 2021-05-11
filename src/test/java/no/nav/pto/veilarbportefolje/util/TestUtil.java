package no.nav.pto.veilarbportefolje.util;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TestUtil {

    public static final String HSQL_URL = "jdbc:hsqldb:mem:portefolje";

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
        Flyway.configure()
                .dataSource(ds)
                .locations("testmigration")
                .skipDefaultResolvers(false)
                .load()
                .migrate();
    }


    private static void setHsqlToOraSyntax(SingleConnectionDataSource ds) {
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SET DATABASE SQL SYNTAX ORA TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
