package no.nav.fo.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
import static no.nav.metrics.MetricsFactory.createTimerProxyForWebService;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

@Configuration
public class AktoerEndpointConfig {

    @Bean
    public AktoerV2 aktoerV2() {
        return factory();
    }

    @Bean
    public Pingable aktoerV2Ping() {
        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
                "Aktoer via SOAP" + System.getProperty("aktoer.endpoint.url"),
                "Sjekker om Aktoer-tjenesten svarer.",
                true
        );

        return () -> {
            try {
                factory().ping();
                return lyktes(metadata);
            } catch (Exception e) {
                return feilet(metadata, e);
            }
        };
    }

    public AktoerV2 factory() {
        return createTimerProxyForWebService("Aktoer_v2", new CXFClient<>(AktoerV2.class)
                .address(getProperty("aktoer.endpoint.url"))
                .configureStsForSystemUserInFSS()
                .build(), AktoerV2.class);
    }
}