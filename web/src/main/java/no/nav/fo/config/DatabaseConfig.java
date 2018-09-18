package no.nav.fo.config;

import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.*;
import no.nav.fo.feed.DialogFeedRepository;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.sbl.jdbc.Transactor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;

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
    public BrukerRepository brukerRepository(JdbcTemplate jdbcTemplate, DataSource ds, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new BrukerRepository(jdbcTemplate, ds, namedParameterJdbcTemplate);
    }

    @Bean
    public PersonRepository personRepository(DataSource ds) {
        return new PersonRepository(ds);
    }

    @Bean
    public OppfolgingFeedRepository OppfolgingFeedRepository(JdbcTemplate db){ return new OppfolgingFeedRepository(db); }

    @Bean
    public DialogFeedRepository dialogFeedRepository(JdbcTemplate db) { return new DialogFeedRepository(db); }

    @Bean
    public AktivitetDAO aktivitetDAO(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource ds) {
        return new AktivitetDAO(db, namedParameterJdbcTemplate, ds);
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
    public KrrRepository krrRepository() { return new KrrRepository(); }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public Transactor transactor(PlatformTransactionManager platformTransactionManager) {
        return new Transactor(platformTransactionManager);
    }

    @Bean
    public Pingable dbPinger(final JdbcTemplate ds) {
        PingMetadata metadata = new PingMetadata(
                "N/A",
                "Database for portefolje",
                true
        );

        return () -> {
            try {
                ds.query("select count(1) from dual", rs->{});
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }
    
    @Bean
    public LockingTaskExecutor taskExecutor(DataSource ds) {
        return new DefaultLockingTaskExecutor(new JdbcLockProvider(ds));
    }

}
