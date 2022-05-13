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

    public static final String OPPFOLGINGS_BRUKER = "veilarbportefolje.oppfolgingsbruker_pa_postgres";
    public static final String HENT_IDENTER_FRA_POSTGRES = "veilarbportefolje.identer_fra_postgres";


    public static final String DIALOG = "veilarbportefolje.dialogPostgres";
    public static final String NOM_SKJERMING = "veilarbportefolje.NOM_Skjerming";
    public static final String PDL_BRUKERDATA = "veilarbportefolje.Pdl_brukerdata";
    public static final String PDL_BRUKERDATA_BACKUP = "veilarbportefolje.Pdl_brukerdata_backup";

    public static boolean brukAvAliasIndeksering(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean brukDialogPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.DIALOG);
    }

    public static boolean brukNOMSkjerming(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.NOM_SKJERMING);
    }
    public static boolean brukOppfolgingsbrukerPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.OPPFOLGINGS_BRUKER);
    }

    public static boolean hentIdenterFraPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.HENT_IDENTER_FRA_POSTGRES);
    }

    public static boolean brukPDLBrukerdata(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.PDL_BRUKERDATA);
    }

    public static boolean brukArenaSomBackup(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.PDL_BRUKERDATA_BACKUP);
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
