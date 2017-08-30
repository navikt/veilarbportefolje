package no.nav.fo.config;

import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.filmottak.tiltak.TiltakRepository;
import no.nav.sbl.dialogarena.common.integrasjon.utils.RowMapper;
import no.nav.sbl.dialogarena.common.integrasjon.utils.SQL;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jndi.JndiTemplate;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Configuration
public class DatabaseConfig {

    public static final String JNDI_NAME = "java:/jboss/datasources/veilarbportefoljeDB";

    @Bean
    public DataSource dataSource() throws ClassNotFoundException, NamingException {
        return new JndiTemplate().lookup(JNDI_NAME, DataSource.class);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws NamingException, SQLException, IOException {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource ds) {
        return new NamedParameterJdbcTemplate(ds);
    }

    @Bean
    public BrukerRepository brukerRepository() {
        return new BrukerRepository();
    }

    @Bean
    public ArbeidslisteRepository arbeidslisteRepository() {
        return new ArbeidslisteRepository();
    }

    @Bean
    public Pingable dbPinger(final DataSource ds) {
        PingMetadata metadata = new PingMetadata(
                "N/A",
                "Database for portefolje",
                true
        );

        return () -> {
            try {
                SQL.query(ds, new RowMapper.IntMapper(), "select count(1) from dual");
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }
}
