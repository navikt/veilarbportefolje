package no.nav.fo.veilarbportefolje.indeksering;

import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.util.EnvironmentUtils;

import static no.nav.sbl.util.EnvironmentUtils.resolveHostName;

@Slf4j
public class ElasticUtils {
    static String getAlias() {
        return String.format("brukerindeks_%s", EnvironmentUtils.requireEnvironmentName());
    }

    static String getElasticHostname() {
        if (onDevillo()) {
            return "tpa-veilarbelastic-elasticsearch.nais.preprod.local";
        } else {
            return "tpa-veilarbelastic-elasticsearch.tpa.svc.nais.local";
        }
    }

    public static boolean onDevillo() {
        boolean onDevillo = resolveHostName().contains("devillo.no");
        log.info("Kjører på devillo.no-domene = {}", onDevillo);
        return onDevillo;
    }
}
