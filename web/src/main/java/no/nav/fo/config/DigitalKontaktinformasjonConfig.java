package no.nav.fo.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

@Configuration
public class DigitalKontaktinformasjonConfig {

    public static String URL = getProperty("dkif.endpoint.url");

    @Bean
    public DigitalKontaktinformasjonV1 dkifV1() {
        return factory();
    }

    @Bean
    public Pingable dkifV1Ping() {
        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
                "DKIF_V1 via " + URL,
                "Ping av DKIF_V1. Henter reservasjon fra KRR.",
                false
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

    private DigitalKontaktinformasjonV1 factory() {
        return new CXFClient<>(DigitalKontaktinformasjonV1.class)
                .address(URL)
                .configureStsForSystemUserInFSS()
                .build();
    }
}
