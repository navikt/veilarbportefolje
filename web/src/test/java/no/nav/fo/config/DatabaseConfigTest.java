package no.nav.fo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class DatabaseConfigTest {

    @Bean
    public javax.sql.DataSource hsqldbDataSource() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:hsqldb:mem:veilarbporteofolje;sql.syntax_ora=true", "sa", "", true);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(javax.sql.DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
