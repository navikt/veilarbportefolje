package no.nav.pto.veilarbportefolje.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.virksomhet.tjenester.enhet.v1.Enhet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.VIRKSOMHET_ENHET_V1_URL_PROPERTY;
import static no.nav.pto.veilarbportefolje.util.PingUtils.ping;
import static no.nav.metrics.MetricsFactory.createTimerProxyForWebService;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class VirksomhetEnhetEndpointConfig {

    private static final String URL = getRequiredProperty(VIRKSOMHET_ENHET_V1_URL_PROPERTY);

    @Bean
    public Enhet virksomhetEnhet() {
        return createTimerProxyForWebService("enhet_v1", new CXFClient<>(Enhet.class)
                .address(URL)
                .configureStsForOnBehalfOfWithJWT()
                .build(), Enhet.class);
    }

    @Bean
    public Pingable virksomhetEnhetPing() {
        Enhet virksomhetEnhet = new CXFClient<>(Enhet.class)
                .address(URL)
                .configureStsForSystemUser()
                .build();

        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "SOAP via " + URL,
                "Tjeneste for å hente ut enheter (NAV Kontor) som veieleder",
                true
        );
        return () -> ping(virksomhetEnhet::ping, metadata);
    }
}
