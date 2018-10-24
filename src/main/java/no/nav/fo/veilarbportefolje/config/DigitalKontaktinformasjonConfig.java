package no.nav.fo.veilarbportefolje.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static java.lang.System.getProperty;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

@Configuration
public class DigitalKontaktinformasjonConfig {

    private static String URL = getProperty(DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY);

    @Bean
    public DigitalKontaktinformasjonV1 dkifV1() {
        return factory();
    }

    @Bean
    public Pingable dkifV1Ping() {
        Pingable.Ping.PingMetadata metadata = new Pingable.Ping.PingMetadata(
                UUID.randomUUID().toString(),
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
                .configureStsForSystemUser()
                .build();
    }
}
