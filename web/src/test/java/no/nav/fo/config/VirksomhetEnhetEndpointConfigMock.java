package no.nav.fo.config;

import no.nav.fo.mock.EnhetMock;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirksomhetEnhetEndpointConfigMock {

    @Bean
    public Enhet virksomhetEnhet() {
        return new EnhetMock();
    }

    @Bean
    public Pingable virksomhetEnhetPing() {
        Enhet virksomhetEnhet = new EnhetMock();

        return () -> {
            try {
                virksomhetEnhet.ping();
                return Pingable.Ping.lyktes("VirksomhetEnhet");
            } catch (Exception e) {
                return Pingable.Ping.feilet("Feilet", e);
            }
        };
    }
}
