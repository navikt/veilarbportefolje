package no.nav.fo.service;

import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class TestPepConfig {
    @Bean
    public Pep pep() { return mock(Pep.class); }

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClientImpl(pep);
    }
}

