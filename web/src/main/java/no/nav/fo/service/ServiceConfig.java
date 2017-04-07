package no.nav.fo.service;

import no.nav.fo.database.PersistentOppdatering;
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
