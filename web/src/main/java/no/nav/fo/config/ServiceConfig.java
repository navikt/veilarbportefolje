package no.nav.fo.config;

import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.service.BrukertilgangService;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.VirksomhetEnhetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public VirksomhetEnhetService virksomhetEnhetServiceImpl() {
        return new VirksomhetEnhetService();
    }

    @Bean
    public BrukertilgangService sjekkBrukertilgang() { return new BrukertilgangService(); }

    @Bean
    public PepClient pepClient() { return new PepClient(); }

    @Bean
    public PersistentOppdatering persistentOppdatering() { return new PersistentOppdatering(); }
}
