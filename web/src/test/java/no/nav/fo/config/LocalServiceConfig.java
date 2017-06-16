package no.nav.fo.config;

import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.mock.AktoerServiceMock;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.VirksomhetEnhetService;
import org.springframework.context.annotation.Bean;

public class LocalServiceConfig {

    @Bean
    public VirksomhetEnhetService virksomhetEnhetServiceImpl() {
        return new VirksomhetEnhetService();
    }

    @Bean
    public BrukertilgangService sjekkBrukertilgang() {
        return new BrukertilgangService();
    }

    @Bean
    public PersistentOppdatering persistentOppdatering() {
        return new PersistentOppdatering();
    }

    @Bean
    public ArbeidslisteService arbeidslisteService() {
        return new ArbeidslisteService();
    }

    @Bean
    public AktoerService aktoerService() { return new AktoerServiceMock(); }

}
