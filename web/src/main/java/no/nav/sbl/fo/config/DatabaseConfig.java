package no.nav.sbl.fo.config;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

/*
@Configuration
public class DatabaseConfig {
    @Bean
    public DataSource oracleDataSource() throws ClassNotFoundException, NamingException {
        return new JndiTemplate().lookup("java:/jboss/datasources/xmlstillingadminDS", DataSource.class);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws NamingException, SQLException, IOException {
        return new JdbcTemplate(dataSource);
    }

//    @Bean
//    public DataSourceTransactionManager transactionManager(DataSource dataSource) throws IOException, SQLException {
//        return new DataSourceTransactionManager(dataSource);
//    }

    @Bean
    public Pingable dbPinger(final DataSource ds) {
        return new Pingable() {
            @Override
            public Pingable.Ping ping() {
                try {
                    SQL.query(ds, new RowMapper.IntMapper(), "select count(1) from dual");
                    return Ping.lyktes("DATABASE");
                } catch (Exception e) {
                    return Ping.feilet("DATABASE", e);
                }
            }
        };
    }

}
*/
