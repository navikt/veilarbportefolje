package no.nav.fo.config;

import no.nav.fo.consumer.OppdaterBrukerdataListener;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.fo.service.PepClientImpl;
import no.nav.fo.service.SolrService;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
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
    public AktoerV2 aktoerV2() {
        return mock(AktoerV2.class);
    }

    @Bean
    public BrukertilgangService brukertilgangService() {
        return new BrukertilgangService();
    }

    @Bean
    public BrukerRepository brukerRepository() {
        return new BrukerRepository();
    }

    @Bean
    public OppdaterBrukerdataFletter oppdaterBrukerdataFletter() {
        return new OppdaterBrukerdataFletter();
    }

    @Bean
    public PersistentOppdatering persistentOppdatering() { return new PersistentOppdatering(); }

    @Bean
    public OppdaterBrukerdataListener oppdaterBrukerdataListener() {
        return new OppdaterBrukerdataListener();
    }

    @Bean
    public SolrService solrService() {
        return mock(SolrService.class);
    }

    @Bean
    public SolrClient solrClient() {
        return mock(HttpSolrClient.class); }

    @Bean
    public Pep pep() { return mock(Pep.class); }

    @Bean
    public PepClientImpl pepClient() { return mock(PepClientImpl.class);
    }
}
