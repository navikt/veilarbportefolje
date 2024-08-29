package no.nav.pto.veilarbportefolje.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static no.nav.pto.veilarbportefolje.util.DbUtils.createDataSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableTransactionManagement
public class DbConfigPostgres {
    private final EnvironmentProperties environmentProperties;

    @Bean
    @Primary
    public DataSource dataSource() {
        return createDataSource(environmentProperties.getDbUrl());
    }

    @Bean
    @Primary
    public JdbcTemplate db(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "PostgresJdbcReadOnly")
    public JdbcTemplate dbRead(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "PostgresNamedJdbcReadOnly")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @PostConstruct
    @SneakyThrows
    public void migrateDb() {
        DataSource dataSource = createDataSource(environmentProperties.getDbUrl());

        if (dataSource != null) {
            log.info("Starting database migration...");
            Flyway.configure()
                    .validateMigrationNaming(true)
                    .dataSource(dataSource)
                    .locations("db/postgres")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();

            dataSource.getConnection().close();
        }
    }
}
