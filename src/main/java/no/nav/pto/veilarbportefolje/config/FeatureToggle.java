package no.nav.pto.veilarbportefolje.config;


import no.finn.unleash.UnleashContext;
import no.nav.pto.veilarbportefolje.service.UnleashService;

import static no.nav.common.featuretoggle.UnleashUtils.resolveUnleashContextFromSubject;

public class FeatureToggle {
    private FeatureToggle() {
    }
    public static final String ARBEIDSLISTA_POSTGRES = "veilarbportefolje.arbeidslista_med_postgres";
    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";

    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_ONPREM_CONSUMERS_STOP = "veilarbportefolje.kafka_onprem_consumers_stop";

    public static boolean erArbeidslistaPaPostgres(UnleashService unleashService, String userId) {
        return unleashService.isEnabled(FeatureToggle.ARBEIDSLISTA_POSTGRES, new UnleashContext(userId, null, null, resolveUnleashContextFromSubject().getProperties()));
    }

    public static boolean brukAvAliasIndeksering(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

}
