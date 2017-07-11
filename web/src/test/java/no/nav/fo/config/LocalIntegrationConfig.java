package no.nav.fo.config;

import no.nav.apiapp.ApiApplication;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.mock.AktoerServiceMock;
import no.nav.fo.mock.EnhetMock;
import no.nav.fo.mock.SolrServiceMock;
import no.nav.fo.service.*;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.apiapp.ApiApplication.Sone.FSS;
import static org.mockito.Mockito.mock;

@Configuration
@Import({
        DatabaseConfigTest.class
})
public class LocalIntegrationConfig implements ApiApplication {
    @Bean
    public AktoerService aktoerService() {
        return new AktoerServiceMock();
    }

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
    public PersistentOppdatering persistentOppdatering() {
        return new PersistentOppdatering();
    }

    @Bean
    public SolrService solrService() {
        return new SolrServiceMock();
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
    public ArbeidslisteService arbeidslisteService() {
        return new ArbeidslisteService();
    }

    @Bean
    public VirksomhetEnhetService virksomhetEnhetService() {
        return new VirksomhetEnhetService();
    }

    @Bean
    public Enhet enhet() {
        return new EnhetMock();
    }


    @Override
    public Sone getSone() {
        return FSS;
    }
}
