package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.fo.config.DatabaseConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.testconfig.ApiAppTest;
import org.eclipse.jetty.plus.jndi.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.dialogarena.config.fasit.FasitUtils.*;
import static no.nav.fo.config.LocalJndiContextConfig.setupDataSourceWithCredentials;
import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.fo.config.feed.AktiviteterfeedConfig.VEILARBAKTIVITET_URL_PROPERTY;
import static no.nav.fo.config.feed.DialogaktorfeedConfig.VEILARBDIALOG_URL_PROPERTY;
import static no.nav.fo.config.feed.OppfolgingerfeedConfig.VEILARBOPPFOLGING_URL_PROPERTY;
import static no.nav.fo.service.VeilederService.VEILARBVEILEDER_URL_PROPERTY;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class StartJettyVeilArbPortefolje {

    public static final String APPLICATION_NAME = "veilarbportefolje";
    private static final String SERVICE_USER_NAME = "srvveilarbportefolje";
    private static final String SECURITY_TOKEN_SERVICE_ALIAS = "securityTokenService";
    private static final String AKTOER_V2_ENDPOINTURL_PROPERTY = "aktoer.endpoint.url";
    private static final String NORG_VIRKSOMHET_ENHET_URL_PROPERTY = "norg.virksomhet_enhet.url";
    private static final String DKIF_ENDPOINT_URL_PROPERTY = "dkif.endpoint.url";
    private static final String VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY = "veilarbportefolje.solr.masternode";
    private static final String VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY = "veilarbportefolje.solr.brukercore.url";

    public static void main(String[] args) throws Exception {

        ServiceUser serviceUser = getServiceUser(SERVICE_USER_NAME, APPLICATION_NAME);

        setProperty(STS_URL_KEY, getBaseUrl(SECURITY_TOKEN_SERVICE_ALIAS, FSS));
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword());

        setProperty(AKTOER_V2_ENDPOINTURL_PROPERTY, "https://app-" + getDefaultEnvironment() + ".adeo.no/aktoerregister/ws/Aktoer/v2");
        setProperty(NORG_VIRKSOMHET_ENHET_URL_PROPERTY, "https://tjenestebuss-" + getDefaultEnvironment() + ".adeo.no/nav-tjeneste-enhet_v1Web/sca/EnhetWSEXP");
        setProperty(DKIF_ENDPOINT_URL_PROPERTY, "https://app-" + getDefaultEnvironment() + ".adeo.no/digital-kontaktinformasjon/DigitalKontaktinformasjon/v1");

        setProperty(VEILARBDIALOG_URL_PROPERTY, "http://localhost:8080/veilarbdialog/api");
        setProperty(VEILARBOPPFOLGING_URL_PROPERTY, "http://localhost:8080/veilarboppfolging/api");
        setProperty(VEILARBAKTIVITET_URL_PROPERTY, "http://localhost:8080/veilarbaktivitet/api");
        setProperty(VEILARBVEILEDER_URL_PROPERTY, "http://localhost:8080/veilarbveileder/api");

        setProperty(VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY, "http://localhost:8080/veilarbportefoljeindeks/brukercore");
        setProperty(VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY, "http://localhost:8080/veilarbportefoljeindeks/brukercore");
        setProperty(UNLEASH_API_URL_PROPERTY_NAME, "https://unleash.nais.adeo.no/api/");

        ServiceUser tiltakSftpUser = getServiceUser("veilarbportefolje.filmottak.sftp.login", APPLICATION_NAME);
        setProperty("veilarbportefolje.filmottak.sftp.login.username", tiltakSftpUser.getUsername());
        setProperty("veilarbportefolje.filmottak.sftp.login.password", tiltakSftpUser.getPassword());

        setProperty("environment.name", getDefaultEnvironment()); // TODO: Remove after bump of common
        setProperty("arena.aktivitet.datofilter", "2017-12-04");

        setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());

        DriverManagerDataSource dataSource = (parseBoolean(getProperty("lokal.database"))) ?
                setupInMemoryDatabase() :
                setupDataSourceWithCredentials(createDbCredentials());

        // TODO slett når common-jetty registerer datasource fornuftig
        new Resource(DatabaseConfig.JNDI_NAME, dataSource);

        //Må ha https for csrf-token
        Jetty jetty = DevelopmentSecurity.setupISSO(
                Jetty.usingWar()
                        .at("veilarbportefolje")
                        .sslPort(9594)
                        .port(9595)
                        .addDatasource(dataSource, DatabaseConfig.JNDI_NAME)
                        .overrideWebXml(),
                new ISSOSecurityConfig(APPLICATION_NAME))
                .configureForJaspic()
                .buildJetty();

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    private static DbCredentials createDbCredentials() {
        return (getProperty("database.url") == null) ? FasitUtils.getDbCredentials(APPLICATION_NAME) :
                new DbCredentials()
                        .setUrl(getProperty("database.url", "jdbc:hsqldb:hsql://localhost/portefolje;ifexists=true"))
                        .setUsername(getProperty("database.brukernavn", "sa"))
                        .setPassword(getProperty("database.passord", ""));
    }

}
