package no.nav.sbl.fo.config.endpoint.norg;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.virksomhet.tjenester.enhet.v1.binding.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirksomhetEnhetEndpointConfig {

    @Bean
    public Enhet virksomhetEnhet() {
        return new CXFClient<>(Enhet.class)
                .address(System.getProperty("norg.virksomhet_enhet.url"))
                .configureStsForExternalSSO()
                .build();
    }

    @Bean
    public Pingable virksomhetEnhetPing() {
        Enhet virksomhetEnhet = new CXFClient<>(Enhet.class)
                .address(System.getProperty("norg.virksomhet_enhet.url"))
                .configureStsForSystemUser()
                .build();

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
