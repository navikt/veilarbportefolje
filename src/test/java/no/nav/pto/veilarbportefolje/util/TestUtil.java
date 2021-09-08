package no.nav.pto.veilarbportefolje.util;

import lombok.SneakyThrows;
import lombok.val;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static java.nio.charset.StandardCharsets.UTF_8;

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
                .locations("db/postgres")
                .baselineOnMigrate(true)
                .load()
                .migrate();
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

    @SneakyThrows
    public static String readFileAsJsonString(String pathname, Class currentLocation) {
        val URI = currentLocation.getResource(pathname).toURI();
        val encodedBytes = Files.readAllBytes(Paths.get(URI));
        return new String(encodedBytes, UTF_8).trim();
    }
}
