package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakRepository;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepository;
import no.nav.pto.veilarbportefolje.database.*;
import no.nav.pto.veilarbportefolje.feed.dialog.DialogFeedRepository;
import no.nav.pto.veilarbportefolje.feed.oppfolging.OppfolgingFeedRepository;
import no.nav.pto.veilarbportefolje.krr.KrrRepository;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.mockito.Mock;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

public class DatabaseConfigTest {

    @Mock
    protected UnleashService unleashService;


    @Bean
    public DataSource hsqldbDataSource() {
      return LocalJndiContextConfig.setupInMemoryDatabase();
    }


    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
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
    public BrukerRepository brukerRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new BrukerRepository(jdbcTemplate, namedParameterJdbcTemplate, unleashService);
    }

    @Bean
    public PersonRepository personRepository(JdbcTemplate jdbcTemplate) {
        return new PersonRepository(jdbcTemplate);
    }

    @Bean
    public OppfolgingFeedRepository OppfolgingFeedRepository(JdbcTemplate db){ return new OppfolgingFeedRepository(db); }

    @Bean
    public DialogFeedRepository dialogFeedRepository(JdbcTemplate db) { return new DialogFeedRepository(db); }

    @Bean
    public TiltakRepository tiltakRepository() { return new TiltakRepository(); }

    @Bean
    public AktivitetDAO aktivitetDAO(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new AktivitetDAO(db, namedParameterJdbcTemplate);
    }

    @Bean
    public KrrRepository krrRepository() { return new KrrRepository(); }
}
