package no.nav.fo.config;

import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.EnhetTiltakRepository;
import no.nav.fo.filmottak.tiltak.TiltakRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DatabaseConfigTest {

    @Bean
    public javax.sql.DataSource hsqldbDataSource() {
      return LocalJndiContextConfig.setupInMemoryDatabase();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(javax.sql.DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(javax.sql.DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public ArbeidslisteRepository arbeidslisteRepository() {
        return new ArbeidslisteRepository();
    }

    @Bean
    public EnhetTiltakRepository enhetTiltakRepository() {
        return new EnhetTiltakRepository();
    }

    @Bean
    public BrukerRepository brukerRepository() { return new BrukerRepository(); }

    @Bean
    public TiltakRepository tiltakRepository() { return new TiltakRepository(); }

    @Bean
    public AktivitetDAO aktivitetDAO() { return new AktivitetDAO(); }
}
