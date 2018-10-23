package no.nav.fo.veilarbportefolje.config;

import no.nav.fo.veilarbportefolje.mock.EnhetMock;
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

        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
                "SOAP via " + System.getProperty("norg.virksomhet_enhet.url"),
                "Tjeneste for å hente ut enheter (NAV Kontor) som veiel",
                true
        );

        return () -> {
            try {
                virksomhetEnhet.ping();
                return Pingable.Ping.lyktes(metadata);
            } catch (Exception e) {
                return Pingable.Ping.feilet(metadata, e);
            }
        };
    }
}
