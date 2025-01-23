package no.nav.pto.veilarbportefolje.config;


import io.getunleash.DefaultUnleash;

public class FeatureToggle {


    private FeatureToggle() {
    }

    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";
    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_SISTE_14A_STOP = "veilarbportefolje.kafka_siste_14a_stop";
    public static final String BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER = "veilarbportefolje.bruk_filter_for_brukerinnsyn_tilganger";
    public static final String STOPP_KONSUMERING_FRA_PORTEFOLJE_HENDELSESFILTER_TOPIC = "veilarbportefolje.stopp_konsumering_fra_portefolje_hendelsesfilter_topic";

    public static boolean brukAvAliasIndeksering(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean brukFilterForBrukerinnsynTilganger(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);
    }
}
