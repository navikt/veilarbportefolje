import no.nav.apiapp.ApiApp;
import no.nav.common.utils.NaisUtils;
import no.nav.fo.veilarbportefolje.config.ApplicationConfig;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.OIDC_REDIRECT_URL_PROPERTY_NAME;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.AKTOER_V2_URL_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.ARENA_AKTIVITET_DATOFILTER_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

public class Main {

    public static void main(String... args) {

        readFromConfigMap();

        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_URL_PROPERTY));
        setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY));
        setProperty(ARENA_AKTIVITET_DATOFILTER_PROPERTY, "2017-12-04");
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/");
        ApiApp.runApp(ApplicationConfig.class, args);
    }

    private static void readFromConfigMap() {
        NaisUtils.addConfigMapToEnv("pto-config",
                "DKIF_URL",
                "SECURITYTOKENSERVICE_URL",
                "ABAC_PDP_ENDPOINT_URL",
                "ABAC_PDP_ENDPOINT_DESCRIPTION",
                "ISSO_HOST_URL",
                "ISSO_JWKS_URL",
                "ISSO_ISSUER_URL",
                "ISSO_ISALIVE_URL",
                "VEILARBLOGIN_REDIRECT_URL_DESCRIPTION",
                "VEILARBLOGIN_REDIRECT_URL_URL",
                "AKTOER_V2_SECURITYTOKEN",
                "AKTOER_V2_ENDPOINTURL",
                "AKTOER_V2_WSDLURL",
                "VIRKSOMHET_OPPFOELGINGSSTATUS_V2_SECURITYTOKEN",
                "VIRKSOMHET_OPPFOELGINGSSTATUS_V2_ENDPOINTURL",
                "VIRKSOMHET_OPPFOELGINGSSTATUS_V2_WSDLURL",
                "LOGINSERVICE_OIDC_CALLBACKURI",
                "LOGINSERVICE_OIDC_DISCOVERYURI",
                "UNLEASH_API_URL",
                "AKTOERREGISTER_API_V1_URL"
        );
    }

}
