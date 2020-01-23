package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.service.VirksomhetEnhetService;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;


@Configuration
public class VirksomhetEnhetConfigTest {

    @Bean
    public VirksomhetEnhetService virksomhetEnhetServiceImpl() { return new VirksomhetEnhetService(); }

    @Bean
    public Enhet virksomhetEnhet() { return mock(Enhet.class);}
}
