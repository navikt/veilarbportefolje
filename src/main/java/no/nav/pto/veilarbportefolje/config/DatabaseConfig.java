package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepository;
import no.nav.pto.veilarbportefolje.database.*;
import no.nav.pto.veilarbportefolje.dialog.DialogFeedRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.krr.KrrRepository;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepository;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepository;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.jdbc.DataSourceFactory;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.UUID;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class DatabaseConfig {

    public static final String VEILARBPORTEFOLJEDB_URL_PROPERTY_NAME = "VEILARBPORTEFOLJEDB_URL";
    public static final String VEILARBPORTEFOLJEDB_USERNAME_PROPERTY_NAME = "VEILARBPORTEFOLJEDB_USERNAME";
    public static final String VEILARBPORTEFOLJEDB_PASSWORD_PROPERTY_NAME = "VEILARBPORTEFOLJEDB_PASSWORD";


    @Bean
    public DataSource dataSource() {
        return DataSourceFactory.dataSource()
                .url(getRequiredProperty(VEILARBPORTEFOLJEDB_URL_PROPERTY_NAME))
                .username(getRequiredProperty(VEILARBPORTEFOLJEDB_USERNAME_PROPERTY_NAME))
                .password(getRequiredProperty(VEILARBPORTEFOLJEDB_PASSWORD_PROPERTY_NAME))
                .maxPoolSize(300)
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource ds) {
        return new NamedParameterJdbcTemplate(ds);
    }


    @Bean
    public BrukerRepository brukerRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate, UnleashService unleashService) {
        return new BrukerRepository(jdbcTemplate, namedParameterJdbcTemplate, unleashService);
    }

    @Bean
    public PersonRepository personRepository(JdbcTemplate jdbcTemplate) {
        return new PersonRepository(jdbcTemplate);
    }

    @Bean
    public OppfolgingRepository OppfolgingFeedRepository(JdbcTemplate db) {
        return new OppfolgingRepository(db);
    }

    @Bean
    public DialogFeedRepository dialogFeedRepository(JdbcTemplate db) {
        return new DialogFeedRepository(db);
    }

    @Bean
    public AktivitetDAO aktivitetDAO(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new AktivitetDAO(jdbcTemplate, namedParameterJdbcTemplate);
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
    public KrrRepository krrRepository() {
        return new KrrRepository();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public Transactor transactor(PlatformTransactionManager platformTransactionManager) {
        return new Transactor(platformTransactionManager);
    }

    @Bean
    public VedtakStatusRepository vedtakStatusRepository(JdbcTemplate jdbcTemplate) {
        return new VedtakStatusRepository(jdbcTemplate);
    }

    @Bean
    public RegistreringRepository registreringRepository(JdbcTemplate jdbcTemplate) {
        return new RegistreringRepository(jdbcTemplate);
    }


    @Bean
    public Pingable dbPinger(final JdbcTemplate db) {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "N/A",
                "Database for portefolje",
                true
        );

        return () -> {
            try {
                db.queryForList("select count(1) from dual");
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }

}
