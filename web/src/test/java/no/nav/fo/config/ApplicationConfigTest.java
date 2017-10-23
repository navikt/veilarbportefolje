package no.nav.fo.config;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.service.*;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        VirksomhetEnhetConfigTest.class,
        DatabaseConfigTest.class
})
public class ApplicationConfigTest {

    @Bean
    public AktoerService aktoerService() {
        return new AktoerServiceImpl();
    }

    @Bean
    public AktorService aktorService() {
        return mock(AktorService.class);
    }

    @Bean
    public BrukertilgangService brukertilgangService() {
        return new BrukertilgangService();
    }

    @Bean
    public AktivitetDAO aktivitetDAO() {
        return new AktivitetDAO();
    }

    @Bean
    public OppdaterBrukerdataFletter oppdaterBrukerdataFletter() {
        return new OppdaterBrukerdataFletter();
    }

    @Bean
    public PersistentOppdatering persistentOppdatering() {
        return new PersistentOppdatering();
    }

    @Bean
    public SolrService solrService() {
        return mock(SolrService.class);
    }

    @Bean
    public SolrClient solrClient() {
        return mock(HttpSolrClient.class);
    }

    @Bean
    public Pep pep() {
        return mock(Pep.class);
    }

    @Bean
    public PepClientImpl pepClient() {
        return mock(PepClientImpl.class);
    }
}
