package no.nav.pto.veilarbportefolje.config;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
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
    public LockingTaskExecutor lockingTaskExecutor() {
        return mock(LockingTaskExecutor.class);
    }
}
