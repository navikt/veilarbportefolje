package no.nav.pto.veilarbportefolje.config;


import no.finn.unleash.UnleashContext;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.common.featuretoggle.UnleashUtils.resolveUnleashContextFromSubject;

public class FeatureToggle {
    private FeatureToggle() {
    }

    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";
    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_ONPREM_CONSUMERS_STOP = "veilarbportefolje.kafka_onprem_consumers_stop";

    public static final String IKKE_AVTALTE_MOTER = "veilarbportfolje.ikke_avtalte_aktiviteter";

    public static final String DIALOG = "veilarbportefolje.dialogPostgres";
    public static final String NOM_SKJERMING = "veilarbportefolje.NOM_Skjerming";

    public static boolean brukAvAliasIndeksering(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean brukDialogPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.DIALOG);
    }

    public static boolean brukNOMSkjerming(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.NOM_SKJERMING);
    }

    public static boolean brukIkkeAvtalteMoter(UnleashService unleashService) {
        try {
            String userId = AuthUtils.getInnloggetVeilederIdent().toString();
            return unleashService.isEnabled(FeatureToggle.IKKE_AVTALTE_MOTER, new UnleashContext(userId, null, null, resolveUnleashContextFromSubject().getProperties()));
        } catch (ResponseStatusException exception) {
            // Dette er ok, ved feks. systemkall og under test
            return unleashService.isEnabled(FeatureToggle.IKKE_AVTALTE_MOTER);
        }
    }
}
