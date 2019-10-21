package no.nav.fo.veilarbportefolje.config;

import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.*;
import no.nav.fo.veilarbportefolje.feed.DialogFeedRepository;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
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
    public static final String DELTAINDEKSERING = "deltaindeksering";
    public static final String TOTALINDEKSERING = "totalindeksering";
    public static final String ES_TOTALINDEKSERING = "es_totalindeksering";
    public static final String ES_DELTAINDEKSERING = "es_deltaindeksering";

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
    public BrukerRepository brukerRepository(JdbcTemplate jdbcTemplate, DataSource ds, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new BrukerRepository(jdbcTemplate, namedParameterJdbcTemplate);
    }

    @Bean
    public PersonRepository personRepository(JdbcTemplate jdbcTemplate) {
        return new PersonRepository(jdbcTemplate);
    }

    @Bean
    public OppfolgingFeedRepository OppfolgingFeedRepository(JdbcTemplate db) {
        return new OppfolgingFeedRepository(db);
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
