package no.nav.pto.veilarbportefolje.config;


import io.getunleash.DefaultUnleash;

public class FeatureToggle {

    private FeatureToggle() {
    }

    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";
    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_SISTE_14A_STOP = "veilarbportefolje.kafka_siste_14a_stop";
    public static final String KAFKA_PORTEFOLJE_AKTIVITET_V1_START = "veilarbportefolje.kafka_portefolje_aktivitet_v1_start";
    public static final String KAFKA_ARBEIDSSOEKER_PERIODE_V3_START_KONSUMERING = "veilarbportefolje.kafka_arbeidssoeker_periode_v3_start_konsumering";
    public static final String KAFKA_ARBEIDSSOEKER_OPPLYSNINGER_V3_START_KONSUMERING = "veilarbportefolje.kafka_arbeidssoeker_opplysninger_v3_start_konsumering";
    public static final String KAFKA_ARBEIDSSOEKER_PROFILERING_V3_START_KONSUMERING = "veilarbportefolje.kafka_arbeidssoeker_profilering_v3_start_konsumering";
    public static final String KAFKA_ARBEIDSSOEKER_PERIODE_V3_STOPP_HENTING_FRA_API = "veilarbportefolje.kafka_arbeidssoeker_periode_v3_stopp_henting_fra_api";
    public static final String BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER = "veilarbportefolje.bruk_filter_for_brukerinnsyn_tilganger";
    public static final String BRUK_TILTAKSAKTIVITET_FRA_AKTIVITETSPLAN = "veilarbportefolje.bruk_tiltaksaktivitet_fra_aktivitetsplan";
    public static final String STOPP_LAGRING_AV_MINE_FILTER = "veilarbportefolje.stopp_lagring_av_mine_filter";
    public static final String STOPP_KJOERENDE_BATCHJOBBER = "veilarbportefolje.stopp_kjoerende_batchjobber";

    public static boolean brukAvAliasIndeksering(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean brukFilterForBrukerinnsynTilganger(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER);
    }

    public static boolean stoppLagringAvMineFilter(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.STOPP_LAGRING_AV_MINE_FILTER);
    }

    public static boolean stoppKjoerendeBatchjobber(DefaultUnleash defaultUnleash) {
        return defaultUnleash.isEnabled(FeatureToggle.STOPP_KJOERENDE_BATCHJOBBER);
    }

}
