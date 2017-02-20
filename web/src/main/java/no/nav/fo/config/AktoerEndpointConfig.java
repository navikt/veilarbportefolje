package no.nav.fo.config;

import no.nav.modig.security.ws.SystemSAMLOutInterceptor;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.aktoer.v2.AktoerV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
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
        return () -> {
            try {
                factory().ping();
                return lyktes("AKTOER_V2");
            } catch (Exception e) {
                return feilet("AKTOER_V2", e);
            }
        };
    }

    public AktoerV2 factory() {
        return new CXFClient<>(AktoerV2.class)
                .address(getProperty("aktoer.endpoint.url"))
                .withOutInterceptor(new SystemSAMLOutInterceptor())
                .build();
    }
}