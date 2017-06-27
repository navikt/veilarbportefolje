package no.nav.fo.config;

import no.nav.fo.database.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LocalJndiContextConfig {

    public static SingleConnectionDataSource setupOracleDataSource() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setUrl("jdbc:oracle:thin:@d26dbfl020.test.local:1521/VEILARBPORTEFOLJE_T6");
        ds.setUsername("VEILARBPORTEFOLJE_T6");
        ds.setPassword("!!CHANGE ME!!");
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
        flyway.setLocations("testmigration");
        flyway.setDataSource(ds);
        flyway.migrate();

        return ds;
    }

}
