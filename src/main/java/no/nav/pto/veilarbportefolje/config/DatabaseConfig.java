package no.nav.pto.veilarbportefolje.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public interface DatabaseConfig {

    DataSource dataSource();

    JdbcTemplate db(DataSource dataSource);

    NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource);

    PlatformTransactionManager transactionManager(DataSource dataSource);
}
