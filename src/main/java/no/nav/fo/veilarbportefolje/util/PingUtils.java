package no.nav.fo.veilarbportefolje.util;

import no.nav.sbl.dialogarena.types.Pingable.Ping;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;

import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

public class PingUtils {

    public static Ping ping(Runnable ping, PingMetadata metadata) {
        try {
            ping.run();
            return lyktes(metadata);
        } catch (Exception e) {
            return feilet(metadata, e);
        }
    }

}
