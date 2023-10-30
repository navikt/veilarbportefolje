package no.nav.pto.veilarbportefolje.config;


import no.nav.pto.veilarbportefolje.service.UnleashService;

public class FeatureToggle {


    private FeatureToggle() {
    }

    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";
    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_SISTE_14A_STOP = "veilarbportefolje.kafka_siste_14a_stop";
    public static final String OPPFOLGINGS_BRUKER = "veilarbportefolje.oppfolgingsbruker_pa_postgres";
    public static final String POAO_TILGANG_ENABLED = "veilarbportefolje.poao-tilgang-enabled";

    public static final String BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER = "veilarbportefolje.bruk_filter_for_brukerinnsyn_tilganger";

    public static final String STOPP_OPENSEARCH_INDEKSERING = "veilarbportefolje.stopp_opensearch_indeksering";

    public static boolean brukAvAliasIndeksering(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean brukOppfolgingsbrukerPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.OPPFOLGINGS_BRUKER);
    }

    public static boolean brukPoaoTilgang(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.POAO_TILGANG_ENABLED);
    }

    public static boolean brukFilterForBrukerinnsynTilganger(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);
    }

    public static boolean stoppOpensearchIndeksering(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.STOPP_OPENSEARCH_INDEKSERING);
    }
}
