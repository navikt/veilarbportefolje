package no.nav.pto.veilarbportefolje.config;


import no.nav.pto.veilarbportefolje.service.UnleashService;

public class FeatureToggle {

    private FeatureToggle() {
    }

    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";
    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_ONPREM_CONSUMERS_STOP = "veilarbportefolje.kafka_onprem_consumers_stop";

    public static final String OPPFOLGINGS_BRUKER = "veilarbportefolje.oppfolgingsbruker_pa_postgres";
    public static final String HENT_IDENTER_FRA_POSTGRES = "veilarbportefolje.identer_fra_postgres";

    public static final String NOM_SKJERMING = "veilarbportefolje.NOM_Skjerming";
    public static final String PDL_BRUKERDATA = "veilarbportefolje.Pdl_brukerdata";
    public static final String PDL_BRUKERDATA_BACKUP = "veilarbportefolje.Pdl_brukerdata_backup";

    public static final String LOG_DIFF_SISTE_ENDRINGER = "veilarbportefolje.logg_diff_siste_endringer";

    private static final String BRUK_SISTE_ENDRINGER_POSTGRES = "veilarbportefolje.bruk_siste_endringer_postgres";

    public static boolean brukAvAliasIndeksering(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean brukNOMSkjerming(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.NOM_SKJERMING);
    }

    public static boolean brukOppfolgingsbrukerPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.OPPFOLGINGS_BRUKER);
    }

    public static boolean loggDiffSisteEndringer(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.LOG_DIFF_SISTE_ENDRINGER);
    }

    public static boolean brukSisteEndringerPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.BRUK_SISTE_ENDRINGER_POSTGRES);
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
}
