package no.nav.fo.config;

import no.nav.fo.mock.AktoerMock;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalAktoerEndpointConfig {

    @Bean
    public AktoerV2 aktoerV2() {
        return new AktoerMock();
    }
}