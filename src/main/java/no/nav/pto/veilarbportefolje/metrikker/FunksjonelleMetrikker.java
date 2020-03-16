package no.nav.pto.veilarbportefolje.metrikker;

import no.nav.metrics.MetricsClient;
import no.nav.metrics.MetricsFactory;
import no.nav.pto.veilarbportefolje.elastic.ElasticUtils;

import static no.nav.metrics.MetricsConfig.resolveNaisConfig;

public class FunksjonelleMetrikker {

    static {
        MetricsClient.enableMetrics(resolveNaisConfig());
    }

    public static void oppdaterAntallBrukere() {
        MetricsFactory
                .createEvent("portefolje.antall.brukere")
                .addFieldToReport("antall_brukere", ElasticUtils.getCount())
                .report();
    }
}
