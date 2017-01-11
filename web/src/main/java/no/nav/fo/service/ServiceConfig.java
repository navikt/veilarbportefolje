package no.nav.fo.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public VirksomhetEnhetServiceImpl virksomhetEnhetServiceImpl() {
        return new VirksomhetEnhetServiceImpl();
    }

    @Bean
    public BrukertilgangService sjekkBrukertilgang() { return new BrukertilgangService(); }
}
