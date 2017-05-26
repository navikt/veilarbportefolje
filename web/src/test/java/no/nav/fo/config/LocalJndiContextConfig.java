package no.nav.fo.config;

import no.nav.fo.database.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class LocalJndiContextConfig {

    public static SingleConnectionDataSource setupOracleDataSource() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setUrl("jdbc:oracle:thin:@d26dbfl020.test.local:1521/VEILARBPORTEFOLJE_T6");
        ds.setUsername("VEILARBPORTEFOLJE_T6");
        ds.setPassword("qLjKfe1LUmyV");
        ds.setSuppressClose(true);
        return ds;
    }

    public static SingleConnectionDataSource setupInMemoryDatabase() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setSuppressClose(true);
        ds.setDriverClassName(TestDriver.class.getName());
        ds.setUrl(TestDriver.URL);
        ds.setUsername("sa");
        ds.setPassword("");

        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SET DATABASE SQL SYNTAX ORA TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarbportefoljeDB");
        flyway.setDataSource(ds);
        int migrate = flyway.migrate();
        assertThat(migrate, greaterThan(0));

        return ds;
    }

}
