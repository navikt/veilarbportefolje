package no.nav.pto.veilarbportefolje.krr;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.NaisUtils.getCredentials;


@Configuration
@Slf4j
public class DigitalKontaktinformasjonConfig {

    @Bean
    public DigitalKontaktinformasjonV1 dkifV1(EnvironmentProperties environmentProperties) {
        Credentials serviceUserCredentials = getCredentials("service_user");
        StsConfig stsConfig = StsConfig.builder()
                .url(environmentProperties.getSoapStsUrl())
                .username(serviceUserCredentials.username)
                .password(serviceUserCredentials.password)
                .build();

        return new CXFClient<>(DigitalKontaktinformasjonV1.class)
                .address(environmentProperties.getDifiUrl())
                .configureStsForSystemUser(stsConfig)
                .build();
    }


    public static HealthCheckResult dkifV1Ping(DigitalKontaktinformasjonV1 dkifV1) {
        try {
            dkifV1.ping();
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            log.error("feil mot dkif " + new RuntimeException(e));
            return HealthCheckResult.unhealthy("Feil mot difi", e);
        }
    }
}
