package no.nav.pto.veilarbportefolje.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

@Slf4j
public class DbUtils {

    public static DataSource createDataSource(String dbUrl) {
        try {
            HikariConfig config = createDataSourceConfig(dbUrl, 15);
            return new HikariDataSource(config);
        } catch (Exception e) {
            log.info("Can't connect to db, error: " + e, e);
            return null;
        }
    }

    public static HikariConfig createDataSourceConfig(String dbUrl, int maximumPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(600000); // 10min
        config.setMinimumIdle(1);
        return config;
    }

    public static String boolToJaNei(boolean bool) {
        return bool ? "J" : "N";
    }
}
