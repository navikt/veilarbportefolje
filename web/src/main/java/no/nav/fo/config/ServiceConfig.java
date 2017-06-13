package no.nav.fo.config;

import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.AktoerServiceImpl;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.VirksomhetEnhetService;
import no.nav.fo.service.impl.ArbeidslisteServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

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
        return new ArbeidslisteServiceImpl();
    }

    @Bean
    public AktoerService aktoerService() { return new AktoerServiceImpl(); }
}
