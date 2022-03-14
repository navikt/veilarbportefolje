package no.nav.pto.veilarbportefolje.config;


import no.nav.pto.veilarbportefolje.service.UnleashService;

public class FeatureToggle {
    private FeatureToggle() {
    }
    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";
    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_ONPREM_CONSUMERS_STOP = "veilarbportefolje.kafka_onprem_consumers_stop";

    public static final String OPPFOLGING_POSTGRES = "veilarbportefolje.oppfolgingsdataPaPostgres";
    public static final String CV_POSTGRES = "veilarbportefolje.cvPaPostgres";

    public static boolean brukAvAliasIndeksering(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean brukAvOppfolgingsdataPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.OPPFOLGING_POSTGRES);
    }

    public static boolean brukAvCvdataPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.CV_POSTGRES);
    }
}
