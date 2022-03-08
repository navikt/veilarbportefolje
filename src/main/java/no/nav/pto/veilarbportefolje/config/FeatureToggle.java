package no.nav.pto.veilarbportefolje.config;


import no.nav.pto.veilarbportefolje.service.UnleashService;

public class FeatureToggle {
    private FeatureToggle() {
    }
    public static final String YTELSER_POSTGRES = "veilarbportefolje.ytelser_med_postgres";
    public static final String ALIAS_INDEKSERING = "veilarbportefolje.aliasIndeksering";
    public static final String ENHETS_TILTAK = "veilarbportefolje.enhetsTiltakPostgres";

    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_ONPREM_CONSUMERS_STOP = "veilarbportefolje.kafka_onprem_consumers_stop";


    public static boolean erYtelserPaPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.YTELSER_POSTGRES);
    }

    public static boolean brukAvAliasIndeksering(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.ALIAS_INDEKSERING);
    }

    public static boolean hentEnhetsTiltakFraPostgres(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.ENHETS_TILTAK);
    }
}
