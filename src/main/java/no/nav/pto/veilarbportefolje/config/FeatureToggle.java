package no.nav.pto.veilarbportefolje.config;


import no.nav.pto.veilarbportefolje.service.UnleashService;

public class FeatureToggle {
    private FeatureToggle() {
    }

    public static final String PDL = "voff.bruk_pdl";

    public static final String AUTO_SLETT = "pto.slett_gamle_aktorer_elastic";

    public static final String POSTGRES = "veilarbportefolje.last_inn_data_pa_postgres";

    public static final String GR202_PA_KAFKA = "veilarbportefolje.GR202_pa_kafka";

    public static final String CV_EKSISTERE_PRODSETTE = "veilarbportefolje.cv_eksistere";

    public static final String KAFKA_AIVEN_CONSUMERS_STOP = "veilarbportefolje.kafka_aiven_consumers_stop";
    public static final String KAFKA_ONPREM_CONSUMERS_STOP = "veilarbportefolje.kafka_onprem_consumers_stop";

    public static boolean erPostgresPa(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.POSTGRES);
    }

    public static boolean erGR202PaKafka(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.GR202_PA_KAFKA);
    }

    public static boolean erCvEksistereIProd(UnleashService unleashService) {
        return unleashService.isEnabled(CV_EKSISTERE_PRODSETTE);
    }
}
