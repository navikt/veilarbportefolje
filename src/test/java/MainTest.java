import no.nav.common.leaderelection.ElectorMock;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.testconfig.ApiAppTest;

import java.util.Optional;

import static java.lang.System.setProperty;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.*;
import static no.nav.pto.veilarbportefolje.config.DatabaseConfig.*;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.HSQL_URL;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class MainTest {

    private static final String PORT = "9595";
    private static final String SERVICE_USER_ALIAS = "srvveilarbportefolje";
    private static final String SECURITY_TOKEN_SERVICE_ALIAS = "securityTokenService";
    private static final String AKTOER_V2_ALIAS = "Aktoer_v2";
    private static final String ABAC_PDP_ENDPOINT_ALIAS = "abac.pdp.endpoint";
    private static final String VEILARBLOGIN_REDIRECT_URL_ALIAS = "veilarblogin.redirect-url";
    private static final String DIGITAL_KONTAKINFORMASJON_V1_ALIAS = "DigitalKontakinformasjon_v1";
    private static final String VIRKSOMHET_ENHET_V1_ALIAS = "virksomhet:Enhet_v1";
    private static final String VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_ALIAS = "veilarbportefolje.arenafiler.sftp.login";

    public static void main(String[] args) {

        setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());

/*
        ServiceUser serviceUser = getServiceUser(SERVICE_USER_ALIAS, APPLICATION_NAME);
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword());
        setProperty("miljo.database", "true");

        DbCredentials dbCredentials = resolveDbCredentials();
        setProperty(VEILARBPORTEFOLJEDB_URL_PROPERTY_NAME, dbCredentials.getUrl());
        setProperty(VEILARBPORTEFOLJEDB_USERNAME_PROPERTY_NAME, dbCredentials.getUsername());
        setProperty(VEILARBPORTEFOLJEDB_PASSWORD_PROPERTY_NAME, dbCredentials.getPassword());

        setProperty(STS_URL_KEY, getBaseUrl(SECURITY_TOKEN_SERVICE_ALIAS, FSS));
        setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, getRestService(ABAC_PDP_ENDPOINT_ALIAS, getDefaultEnvironment()).getUrl());
        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.getPassword());
        setProperty(AKTOER_V2_URL_PROPERTY, getWebServiceEndpoint(AKTOER_V2_ALIAS).getUrl());
        setProperty(VIRKSOMHET_ENHET_V1_URL_PROPERTY, getWebServiceEndpoint(VIRKSOMHET_ENHET_V1_ALIAS).getUrl());
        setProperty(DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY, getWebServiceEndpoint(DIGITAL_KONTAKINFORMASJON_V1_ALIAS).getUrl());

        setProperty(VEILARBDIALOG_URL_PROPERTY, "http://localhost:8080/veilarbdialog/api");
        setProperty(VEILARBOPPFOLGING_URL_PROPERTY, "http://localhost:8080/veilarboppfolging/api");
        setProperty(VEILARBAKTIVITET_URL_PROPERTY, "http://localhost:8080/veilarbaktivitet/api");

        ServiceUser tiltakSftpUser = getServiceUser(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_ALIAS, APPLICATION_NAME);
        setProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD, tiltakSftpUser.getPassword());
        setProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME, tiltakSftpUser.getUsername());

        ServiceUser isso_rp_user = getServiceUser("isso-rp-user", APPLICATION_NAME);
        String loginUrl = getRestService(VEILARBLOGIN_REDIRECT_URL_ALIAS, getDefaultEnvironment()).getUrl();

        setProperty(ISSO_HOST_URL_PROPERTY_NAME, getBaseUrl("isso-host"));
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername());
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword());
        setProperty(ISSO_JWKS_URL_PROPERTY_NAME, getBaseUrl("isso-jwks"));
        setProperty(ISSO_ISSUER_URL_PROPERTY_NAME, getBaseUrl("isso-issuer"));
        setProperty(ISSO_ISALIVE_URL_PROPERTY_NAME, getBaseUrl("isso.isalive", Zone.FSS));
        setProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY, loginUrl);

        ServiceUser elasticUser = getServiceUser("veilarbelastic_user", APPLICATION_NAME, getDefaultEnvironment());
        setProperty(ELASTICSEARCH_USERNAME_PROPERTY, elasticUser.getUsername());
        setProperty(ELASTICSEARCH_PASSWORD_PROPERTY, elasticUser.getPassword());
        */

        ElectorMock.start();
        Main.main(PORT);
    }
/*
    private static DbCredentials resolveDbCredentials() {
        Optional<String> miljoDatabaseProperty = getOptionalProperty("miljo.database");
        if (miljoDatabaseProperty.isPresent() && "true".equals(miljoDatabaseProperty.get())) {
            return getDbCredentials(APPLICATION_NAME);
        } else {
            DbCredentials dbCredentials = new DbCredentials().setUrl(HSQL_URL)
                    .setUsername("sa")
                    .setPassword("pw");
            setupDataSourceWithCredentials(dbCredentials);
            setProperty(SKIP_DB_MIGRATION_PROPERTY, "true");
            return dbCredentials;
        }
    }*/
}
