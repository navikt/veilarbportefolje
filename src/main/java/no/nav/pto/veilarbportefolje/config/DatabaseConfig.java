package no.nav.pto.veilarbportefolje.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.utils.Credentials;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.UUID;

import static no.nav.common.utils.NaisUtils.getCredentials;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Autowired
    private EnvironmentProperties environmentProperties;


    @Bean
    public DataSource dataSource() {
        Credentials oracleCredentials = getCredentials("oracle_creds");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environmentProperties.getDbUrl());
        config.setUsername(oracleCredentials.username);
        config.setPassword(oracleCredentials.password);
        config.setMaximumPoolSize(300);
        DataSource dataSource = new HikariDataSource(config);

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();

        return dataSource;
    }

    @Bean
    public JdbcTemplate db(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource ds) {
        return new NamedParameterJdbcTemplate(ds);
    }

    /*
    @Bean
    public Pingable dbPinger(final JdbcTemplate db) {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "N/A",
                "Database for portefolje",
                true
        );

        return () -> {
            try {
                db.queryForList("select count(1) from dual");
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }


     */

}
