package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.TestUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;


public class DatabaseConfigTest {


    @Bean
    public DataSource hsqldbDataSource() {
      return TestUtil.setupInMemoryDatabase();
    }


    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

}
