package no.nav.fo.veilarbportefolje.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY;
import static no.nav.fo.veilarbportefolje.util.PingUtils.ping;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class DigitalKontaktinformasjonConfig {

    private static String URL = getRequiredProperty(DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY);

    @Bean
    public DigitalKontaktinformasjonV1 dkifV1() {
        return digitalKontaktinformasjon();
    }

    @Bean
    public Pingable dkifV1Ping() {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "DKIF_V1 via " + URL,
                "Ping av DKIF_V1. Henter reservasjon fra KRR.",
                false
        );
        return () -> ping(() -> digitalKontaktinformasjon().ping(), metadata);
    }

    private DigitalKontaktinformasjonV1 digitalKontaktinformasjon() {
        return new CXFClient<>(DigitalKontaktinformasjonV1.class)
                .address(URL)
                .configureStsForSystemUser()
                .build();
    }
}
