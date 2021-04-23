package no.nav.pto.veilarbportefolje.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public interface DatabaseConfig {

    DataSource dataSource();

    JdbcTemplate db(@Qualifier("Oracle") DataSource dataSource);

    NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource);

    PlatformTransactionManager transactionManager(DataSource dataSource);
}
