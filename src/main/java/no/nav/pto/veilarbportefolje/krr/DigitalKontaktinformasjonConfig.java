package no.nav.pto.veilarbportefolje.krr;

import no.nav.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY;
import static no.nav.pto.veilarbportefolje.util.PingUtils.ping;

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
