package no.nav.pto.veilarbportefolje.util;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
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

    public static void testMigrate (DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("db/migrationPostgres")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    private static void migrateDb(DriverManagerDataSource ds) {
        /*
        Flyway flyway = new Flyway();
        flyway.setSkipDefaultResolvers(true);
        flyway.setResolvers(new MergeMigrationResolver());
        flyway.setDataSource(ds);
        flyway.migrate();
        */
        Flyway.configure()
                .dataSource(ds)
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
