import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.testconfig.ApiAppTest;

import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.*;
import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.*;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class MainTest {

    private static final String PORT = "9595";
    private static final String SERVICE_USER_ALIAS = "srvveilarbportefolje";
    private static final String SECURITY_TOKEN_SERVICE_ALIAS = "securityTokenService";
    private static final String AKTOER_V2_ALIAS = "Aktoer_v2";
    private static final String ABAC_PDP_ENDPOINT_ALIAS = "abac.pdp.endpoint";
    private static final String UNLEASH_API_ALIAS = "unleash-api";
    private static final String VEILARBLOGIN_REDIRECT_URL_ALIAS = "veilarblogin.redirect-url";
    private static final String DIGITAL_KONTAKINFORMASJON_V1_ALIAS = "DigitalKontakinformasjon_v1";
    private static final String VIRKSOMHET_ENHET_V1_ALIAS = "virksomhet:Enhet_v1";
    private static final String VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_ALIAS = "veilarbportefolje.filmottak.sftp.login";

    public static void main(String[] args) {

        setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());

        ServiceUser serviceUser = getServiceUser(SERVICE_USER_ALIAS, APPLICATION_NAME);
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword());

        // TODO: Støtte inMemoryDb eller lokal hsqldb ala jdbc:hsqldb:hsql://localhost/portefolje;ifexists=true
        DbCredentials dbCredentials = getDbCredentials(APPLICATION_NAME);
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
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, getRestService(UNLEASH_API_ALIAS, getDefaultEnvironment()).getUrl());

        setProperty(VEILARBDIALOG_URL_PROPERTY, "http://localhost:8080/veilarbdialog/api");
        setProperty(VEILARBOPPFOLGING_URL_PROPERTY, "http://localhost:8080/veilarboppfolging/api");
        setProperty(VEILARBAKTIVITET_URL_PROPERTY, "http://localhost:8080/veilarbaktivitet/api");
        setProperty(VEILARBVEILEDER_URL_PROPERTY, "http://localhost:8080/veilarbveileder/api");
        setProperty(VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY, "http://localhost:8080/veilarbportefoljeindeks/brukercore");
        setProperty(VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY, "http://localhost:8080/veilarbportefoljeindeks/brukercore");

        ServiceUser tiltakSftpUser = getServiceUser(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_ALIAS, APPLICATION_NAME);
        setProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME_PROPERTY, tiltakSftpUser.getUsername());
        setProperty(VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD_PROPERTY, tiltakSftpUser.getPassword());

        setProperty(LOEPENDEYTELSER_PATH_PROPERTY, "/");
        setProperty(LOEPENDEYTELSER_FILNAVN_PROPERTY, "loependeytelser.xml");

        ServiceUser isso_rp_user = getServiceUser("isso-rp-user", APPLICATION_NAME);
        String loginUrl = getRestService(VEILARBLOGIN_REDIRECT_URL_ALIAS, getDefaultEnvironment()).getUrl();

        setProperty(ISSO_HOST_URL_PROPERTY_NAME, getBaseUrl("isso-host"));
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername());
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword());
        setProperty(ISSO_JWKS_URL_PROPERTY_NAME, getBaseUrl("isso-jwks"));
        setProperty(ISSO_ISSUER_URL_PROPERTY_NAME, getBaseUrl("isso-issuer"));
        setProperty(ISSO_ISALIVE_URL_PROPERTY_NAME, getBaseUrl("isso.isalive", Zone.FSS));
        setProperty(VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY, loginUrl);

        Main.main(PORT);
    }
}
