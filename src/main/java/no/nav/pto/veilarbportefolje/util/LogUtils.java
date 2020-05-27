package no.nav.pto.veilarbportefolje.util;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;

import static java.lang.String.format;

@Slf4j
public class LogUtils {
    public static void logErrorWithStacktrace(OppfolgingsBruker bruker, String feilType) {
        String msg = format(feilType + ": bruker %s (personId %s)", bruker.getAktoer_id(), bruker.getPerson_id());
        log.error(msg, new RuntimeException());
    }
}
