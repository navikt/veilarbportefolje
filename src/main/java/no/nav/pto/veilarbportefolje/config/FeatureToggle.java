package no.nav.pto.veilarbportefolje.config;


import io.getunleash.DefaultUnleash;

public class FeatureToggle {


    private FeatureToggle() {
    }

    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";
    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_SISTE_14A_STOP = "veilarbportefolje.kafka_siste_14a_stop";
    public static final String OPPFOLGINGS_BRUKER = "veilarbportefolje.oppfolgingsbruker_pa_postgres";
    public static final String BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER = "veilarbportefolje.bruk_filter_for_brukerinnsyn_tilganger";

    public static final String STOPP_OPENSEARCH_INDEKSERING = "veilarbportefolje.stopp_opensearch_indeksering";

    public static boolean brukAvAliasIndeksering(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean brukOppfolgingsbrukerPaPostgres(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.OPPFOLGINGS_BRUKER);
    }

    public static boolean brukFilterForBrukerinnsynTilganger(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);
    }

    public static boolean stoppOpensearchIndeksering(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.STOPP_OPENSEARCH_INDEKSERING);
    }
}
