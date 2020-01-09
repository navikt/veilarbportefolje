import no.nav.apiapp.ApiApp;
import no.nav.brukerdialog.tools.SecurityConstants;
import no.nav.common.utils.NaisUtils;
import no.nav.fo.veilarbportefolje.config.ApplicationConfig;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.OIDC_REDIRECT_URL_PROPERTY_NAME;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.dialogarena.aktor.AktorConfig.AKTOER_ENDPOINT_URL;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.AKTOER_V2_URL_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.ARENA_AKTIVITET_DATOFILTER_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class Main {

    public static void main(String... args) {

        NaisUtils.Credentials serviceUser = NaisUtils.getCredentials("service_user");

        //ABAC
        System.setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //CXF
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        //OIDC
        System.setProperty(SecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(SecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        setProperty(AKTOER_ENDPOINT_URL, getRequiredProperty(AKTOER_V2_URL_PROPERTY));
        setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, getRequiredProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY));
        setProperty(ARENA_AKTIVITET_DATOFILTER_PROPERTY, "2017-12-04");
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/");


        NaisUtils.Credentials oracleCreds = getCredentials("oracle_creds");
        System.setProperty("VEILARBPORTEFOLJEDB_USERNAME", oracleCreds.username);
        System.setProperty("VEILARBPORTEFOLJEDB_PASSWORD", oracleCreds.password);

        NaisUtils.addConfigMapToEnv("pto-config",
                "ABAC_PDP_ENDPOINT_DESCRIPTION",
                "ABAC_PDP_ENDPOINT_URL",
                "AKTOER_V2_ENDPOINTURL",
                "AKTOER_V2_SECURITYTOKEN",
                "AKTOER_V2_WSDLURL",
                "APPDYNAMICS_CONTROLLER_HOST_NAME",
                "APPDYNAMICS_AGENT_ACCOUNT_NAME",
                "APPDYNAMICS_CONTROLLER_PORT",
                "APPDYNAMICS_CONTROLLER_SSL_ENABLED",
                "ISSO_HOST_URL",
                "ISSO_ISALIVE_URL",
                "ISSO_ISSUER_URL",
                "ISSO_JWKS_URL",
                "SECURITY_TOKEN_SERVICE_OPENID_CONFIGURATION_URL",
                "SECURITYTOKENSERVICE_URL",
                "UNLEASH_API_URL",
                "VEILARBAKTIVITETAPI_URL",
                "VEILARBDIALOGAPI_URL",
                "VEILARBLOGIN_REDIRECT_URL_DESCRIPTION",
                "VEILARBLOGIN_REDIRECT_URL_URL",
                "VEILARBOPPFOLGINGAPI_URL",
                "VEILARBVEILEDERAPI_URL",
                "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ENDPOINTURL",
                "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_SECURITYTOKEN",
                "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_WSDLURL",
                "VIRKSOMHET_ENHET_V1_ENDPOINTURL",
                "VIRKSOMHET_ENHET_V1_SECURITYTOKEN",
                "VIRKSOMHET_ENHET_V1_WSDLURL"
        );

        ApiApp.runApp(ApplicationConfig.class, args);
    }

}
