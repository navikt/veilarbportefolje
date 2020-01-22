package no.nav.pto.veilarbportefolje.config;

import lombok.SneakyThrows;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.pto.veilarbportefolje.abac.PepClientImpl;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.*;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        VirksomhetEnhetConfigTest.class,
        DatabaseConfigTest.class
})
public class ApplicationConfigTest {

    @Bean
    public ElasticIndexer elasticIndexer() {
        return mock(ElasticIndexer.class);
    }

    @Bean
    public AktoerService aktoerService() {
        return new AktoerServiceImpl();
    }

    @Bean
    public AktorService aktorService() {
        return mock(AktorService.class);
    }

    @Bean
    public AktivitetDAO aktivitetDAO(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new AktivitetDAO(db, namedParameterJdbcTemplate);
    }

    @Bean
    public PersistentOppdatering persistentOppdatering() {
        return new PersistentOppdatering();
    }

    @Bean
    public VeilederService veilederService() {
        return mock(VeilederService.class);
    }

    @Bean
    public Pep pep() {
        return mock(Pep.class);
    }

    @Bean
    public PepClientImpl pepClient() {
        return mock(PepClientImpl.class);
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor() {
        return mock(LockingTaskExecutor.class);
    }

    @Bean
    public Transactor transactor() {
        class TestTransactor extends Transactor {

            public TestTransactor() {
                super(null);
            }

            @Override
            @SneakyThrows
            public void inTransaction(InTransaction inTransaction) {
                inTransaction.run();
            }

        }
        return new TestTransactor();
    }
}
