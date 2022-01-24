package no.nav.pto.veilarbportefolje.config;


import no.finn.unleash.UnleashContext;
import no.nav.pto.veilarbportefolje.service.UnleashService;

import static no.nav.common.featuretoggle.UnleashUtils.resolveUnleashContextFromSubject;

public class FeatureToggle {
    private FeatureToggle() {
    }

    public static final String PDL = "voff.bruk_pdl";

    public static final String AUTO_SLETT = "pto.slett_gamle_aktorer_elastic";

    public static final String POSTGRES = "veilarbportefolje.sok_med_postgres";
    public static final String IKKE_AVTALT = "veilarbportefolje.bruk_ikke_avtalte_aktiviteter";

    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_ONPREM_CONSUMERS_STOP = "veilarbportefolje.kafka_onprem_consumers_stop";

    public static boolean erPostgresPa(UnleashService unleashService, String userId) {
        return unleashService.isEnabled(FeatureToggle.POSTGRES, new UnleashContext(userId, null, null, resolveUnleashContextFromSubject().getProperties()));
    }

    public static boolean brukIkkeAvtalteAktiviteter(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.IKKE_AVTALT);
    }

}
