package no.nav.pto.veilarbportefolje.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.utils.Credentials;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.common.utils.NaisUtils.getFileContent;

@Configuration
@EnableTransactionManagement
public class DbConfigOracle implements DatabaseConfig {
    private final String oracleURL;

    public DbConfigOracle() {
        this.oracleURL = getFileContent("/var/run/secrets/nais.io/oracle_config/jdbc_url");
    }


    @Bean
    public DataSource dataSource() {
        Credentials oracleCredentials = getCredentials("oracle_creds");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(oracleURL);
        config.setUsername(oracleCredentials.username);
        config.setPassword(oracleCredentials.password);
        config.setMaximumPoolSize(300);
        DataSource dataSource = new HikariDataSource(config);

        migrateDb(dataSource);

        return dataSource;
    }

    @Bean
    @Override
    public JdbcTemplate db(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Override
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    public static HealthCheckResult dbPinger(final JdbcTemplate db) {
        try {
            db.queryForList("select count(1) from dual");
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Feil mot databasen", e);
        }
    }


    private static void migrateDb(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }
}
