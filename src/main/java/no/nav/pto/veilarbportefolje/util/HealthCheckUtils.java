package no.nav.pto.veilarbportefolje.util;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.sbl.dialogarena.types.Pingable.Ping;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;

import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

public class HealthCheckUtils {

    public static HealthCheckResult ping(Runnable ping, PingMetadata metadata) {
        try {
            ping.run();
            return lyktes(metadata);
        } catch (Exception e) {
            return feilet(metadata, e);
        }
    }

}
