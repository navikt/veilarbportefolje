package no.nav.sbl.fo.config;

import no.nav.sbl.dialogarena.common.integrasjon.utils.RowMapper;
import no.nav.sbl.dialogarena.common.integrasjon.utils.SQL;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Configuration
public class MockDatabaseConfig {
    @Bean
    public DataSource hsqldbDataSource() throws IOException {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:hsqldb:mem:veilarbportefolje;sql.syntax_ora=true", "sa", "", true);
//        createTables(dataSource);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws NamingException, SQLException, IOException {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public Pingable dbPinger(final DataSource ds) {
        return new Pingable() {
            @Override
            public Pingable.Ping ping() {
                try {
                    SQL.query(ds, new RowMapper.IntMapper(), "select count(1) from dual");
                    return Ping.lyktes("DATABASE");
                } catch (Exception e) {
                    return Pingable.Ping.feilet("DATABASE", e);
                }
            }
        };
    }
}
