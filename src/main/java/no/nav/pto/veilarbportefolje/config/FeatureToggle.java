package no.nav.pto.veilarbportefolje.config;

import no.nav.common.featuretoggle.UnleashService;

public class FeatureToggle {
    private FeatureToggle() {
    }

    public static final String PDL = "veilarbmaofs.personalia.pdl.persondata";

    public static final String AUTO_SLETT = "pto.slett_gamle_aktorer_elastic";

    public static final String POSTGRES = "veilarbportefolje.last_inn_data_pa_postgres";

    public static final String FIKS_NY_FOR_VEILEDER = "veilarbportefolje.fiks_ny_for_veileder";

    public static final String GR202_PA_KAFKA = "veilarbportefolje.GR202_pa_kafka";

    public static boolean erPostgresPa(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.POSTGRES);
    }

    public static boolean erGR202PaKafka(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.GR202_PA_KAFKA);
    }
}
