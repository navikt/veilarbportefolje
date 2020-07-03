package no.nav.pto.veilarbportefolje.krr;

import no.nav.common.cxf.CXFClient;
import no.nav.common.health.HealthCheckResult;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DigitalKontaktinformasjonConfig {

    @Bean
    public DigitalKontaktinformasjonV1 dkifV1(EnvironmentProperties environmentProperties) {
        return digitalKontaktinformasjon(environmentProperties.getDifiUrl());
    }

    private DigitalKontaktinformasjonV1 digitalKontaktinformasjon(String difiUrl) {
        return new CXFClient<>(DigitalKontaktinformasjonV1.class)
                .address(difiUrl)
                .configureStsForSystemUser()
                .build();
    }


    public static HealthCheckResult dkifV1Ping(DigitalKontaktinformasjonV1 dkifV1) {
        try {
            dkifV1.ping();
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Feil mot difi", e);
        }
    }
}
