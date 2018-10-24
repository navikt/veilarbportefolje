import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.testconfig.ApiAppTest;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static no.nav.brukerdialog.security.Constants.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbportefolje.config.DatabaseConfig.*;
import static no.nav.fo.veilarbportefolje.config.feed.AktiviteterfeedConfig.VEILARBAKTIVITET_URL_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.feed.DialogaktorfeedConfig.VEILARBDIALOG_URL_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.feed.OppfolgingerfeedConfig.VEILARBOPPFOLGING_URL_PROPERTY;
import static no.nav.fo.veilarbportefolje.service.VeilederService.VEILARBVEILEDER_URL_PROPERTY;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class MainTest {

    private static final String PORT = "9595";
    private static final String SERVICE_USER_NAME = "srvveilarbportefolje";
    private static final String SECURITY_TOKEN_SERVICE_ALIAS = "securityTokenService";
    private static final String AKTOER_V2_ENDPOINTURL_PROPERTY = "aktoer.endpoint.url";
    private static final String NORG_VIRKSOMHET_ENHET_URL_PROPERTY = "norg.virksomhet_enhet.url";
    private static final String DKIF_ENDPOINT_URL_PROPERTY = "dkif.endpoint.url";
    private static final String VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY = "veilarbportefolje.solr.masternode";
    private static final String VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY = "veilarbportefolje.solr.brukercore.url";
    private static final String ABAC_PDP_ENDPOINT_ALIAS = "abac.pdp.endpoint";

    public static void main(String[] args) {

        setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());

        ServiceUser serviceUser = getServiceUser(SERVICE_USER_NAME, APPLICATION_NAME);

        setProperty(STS_URL_KEY, getBaseUrl(SECURITY_TOKEN_SERVICE_ALIAS, FSS));
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword());

        DbCredentials dbCredentials = FasitUtils.getDbCredentials(APPLICATION_NAME);
        setProperty(VEILARBPORTEFOLJEDB_URL_PROPERTY_NAME, dbCredentials.getUrl());
        setProperty(VEILARBPORTEFOLJEDB_USERNAME_PROPERTY_NAME, dbCredentials.getUsername());
        setProperty(VEILARBPORTEFOLJEDB_PASSWORD_PROPERTY_NAME, dbCredentials.getPassword());

        setProperty(ABAC_ENDPOINT_URL_PROPERTY_NAME, FasitUtils.getRestService(ABAC_PDP_ENDPOINT_ALIAS, getDefaultEnvironment()).getUrl());
        setProperty(CredentialConstants.SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(CredentialConstants.SYSTEMUSER_PASSWORD, serviceUser.getPassword());

        setProperty(AKTOER_V2_ENDPOINTURL_PROPERTY, "https://app-" + getDefaultEnvironment() + ".adeo.no/aktoerregister/ws/Aktoer/v2");
        setProperty(NORG_VIRKSOMHET_ENHET_URL_PROPERTY, "https://tjenestebuss-" + getDefaultEnvironment() + ".adeo.no/nav-tjeneste-enhet_v1Web/sca/EnhetWSEXP");
        setProperty(DKIF_ENDPOINT_URL_PROPERTY, "https://app-" + getDefaultEnvironment() + ".adeo.no/digital-kontaktinformasjon/DigitalKontaktinformasjon/v1");
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/");

        setProperty(VEILARBDIALOG_URL_PROPERTY, "http://localhost:8080/veilarbdialog/api");
        setProperty(VEILARBOPPFOLGING_URL_PROPERTY, "http://localhost:8080/veilarboppfolging/api");
        setProperty(VEILARBAKTIVITET_URL_PROPERTY, "http://localhost:8080/veilarbaktivitet/api");
        setProperty(VEILARBVEILEDER_URL_PROPERTY, "http://localhost:8080/veilarbveileder/api");
        setProperty(VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY, "http://localhost:8080/veilarbportefoljeindeks/brukercore");
        setProperty(VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY, "http://localhost:8080/veilarbportefoljeindeks/brukercore");

        ServiceUser tiltakSftpUser = getServiceUser("veilarbportefolje.filmottak.sftp.login", APPLICATION_NAME);
        setProperty("veilarbportefolje.filmottak.sftp.login.username", tiltakSftpUser.getUsername());
        setProperty("veilarbportefolje.filmottak.sftp.login.password", tiltakSftpUser.getPassword());

        setProperty("environment.name", getDefaultEnvironment()); // TODO: Remove after bump of common
        setProperty("arena.aktivitet.datofilter", "2017-12-04");
        setProperty("loependeytelser.path", "/");
        setProperty("loependeytelser.filnavn", "loependeytelser.xml");

        ServiceUser isso_rp_user = FasitUtils.getServiceUser("isso-rp-user", APPLICATION_NAME);
        String loginUrl = getRestService("veilarblogin.redirect-url", getDefaultEnvironment()).getUrl();

        setProperty(ISSO_HOST_URL_PROPERTY_NAME, getBaseUrl("isso-host"));
        setProperty(ISSO_RP_USER_USERNAME_PROPERTY_NAME, isso_rp_user.getUsername());
        setProperty(ISSO_RP_USER_PASSWORD_PROPERTY_NAME, isso_rp_user.getPassword());
        setProperty(ISSO_JWKS_URL_PROPERTY_NAME, getBaseUrl("isso-jwks"));
        setProperty(ISSO_ISSUER_URL_PROPERTY_NAME, getBaseUrl("isso-issuer"));
        setProperty(ISSO_ISALIVE_URL_PROPERTY_NAME, getBaseUrl("isso.isalive", Zone.FSS));
        setProperty("VEILARBLOGIN_REDIRECT_URL_URL", loginUrl);
        setProperty(OIDC_REDIRECT_URL_PROPERTY_NAME, loginUrl);

        Main.main(PORT);
    }

    private static DbCredentials createDbCredentials() {
        return (getProperty("database.url") == null) ? FasitUtils.getDbCredentials(APPLICATION_NAME) :
                new DbCredentials()
                        .setUrl(getProperty("database.url", "jdbc:hsqldb:hsql://localhost/portefolje;ifexists=true"))
                        .setUsername(getProperty("database.brukernavn", "sa"))
                        .setPassword(getProperty("database.passord", ""));
    }

}
