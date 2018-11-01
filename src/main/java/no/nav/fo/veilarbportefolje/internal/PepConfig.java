package no.nav.fo.veilarbportefolje.internal;


import lombok.SneakyThrows;
import no.nav.brukerdialog.security.pingable.IssoIsAliveHelsesjekk;
import no.nav.brukerdialog.security.pingable.IssoSystemBrukerTokenHelsesjekk;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;
import java.util.UUID;

import static no.nav.fo.veilarbportefolje.util.PingUtils.ping;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class PepConfig {

    @Inject
    private PepClient pep;

    @Bean
    public Pingable pepPing() {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "ABAC via " + getRequiredProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME),
                "Tilgangskontroll, sjekk om NAV-ansatt har tilgang til bruker.",
                true
        );

        return () -> ping(this::doPing, metadata);
    }

    @SneakyThrows
    private void doPing() {
        pep.ping();
    }

    @Bean
    public Pingable issoPing() {
        return new IssoIsAliveHelsesjekk();
    }

    @Bean
    public Pingable SystemBrukerToken() {
        return new IssoSystemBrukerTokenHelsesjekk();
    }
}
