package no.nav.fo.config.endpoints;

import no.nav.fo.service.VirksomhetEnhetServiceImpl;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;


@Configuration
public class VirksomhetEnhetTestConfig {

    @Bean
    public VirksomhetEnhetServiceImpl virksomhetEnhetServiceImpl() { return new VirksomhetEnhetServiceImpl(); }

    @Bean
    public Enhet virksomhetEnhet() { return mock(Enhet.class);}
}
