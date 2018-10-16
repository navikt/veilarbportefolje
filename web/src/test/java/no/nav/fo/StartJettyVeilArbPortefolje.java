package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.ServiceUser;
import no.nav.fo.config.DatabaseConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.eclipse.jetty.plus.jndi.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static no.nav.dialogarena.config.fasit.FasitUtils.Zone.FSS;
import static no.nav.dialogarena.config.fasit.FasitUtils.getDefaultEnvironment;
import static no.nav.fo.config.LocalJndiContextConfig.setupDataSourceWithCredentials;
import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.fo.config.feed.AktiviteterfeedConfig.VEILARBAKTIVITET_URL_PROPERTY;
import static no.nav.fo.config.feed.DialogaktorfeedConfig.VEILARBDIALOG_URL_PROPERTY;
import static no.nav.fo.config.feed.OppfolgingerfeedConfig.VEILARBOPPFOLGING_URL_PROPERTY;
import static no.nav.fo.service.VeilederService.VEILARBVEILEDER_URL_PROPERTY;
import static no.nav.modig.lang.collections.FactoryUtils.gotKeypress;
import static no.nav.modig.lang.collections.RunnableUtils.first;
import static no.nav.modig.lang.collections.RunnableUtils.waitFor;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class StartJettyVeilArbPortefolje {

    public static final String APPLICATION_NAME = "veilarbportefolje";
    public static final String SERVICE_USER_NAME = "srvveilarbportefolje";
    private static final String AKTOER_V2_ENDPOINTURL_PROPERTY = "aktoer.endpoint.url";

    public static void main(String[] args) throws Exception {

        String securityTokenService = FasitUtils.getBaseUrl("securityTokenService", FSS);
        ServiceUser serviceUser = FasitUtils.getServiceUser(SERVICE_USER_NAME, APPLICATION_NAME);

        setProperty(STS_URL_KEY, securityTokenService);
        setProperty(SYSTEMUSER_USERNAME, serviceUser.getUsername());
        setProperty(SYSTEMUSER_PASSWORD, serviceUser.getPassword());

        setProperty(AKTOER_V2_ENDPOINTURL_PROPERTY, "https://app-" + getDefaultEnvironment() + ".adeo.no/aktoerregister/ws/Aktoer/v2");

        setProperty(VEILARBDIALOG_URL_PROPERTY, "http://localhost:8080/veilarbdialog/api");
        setProperty(VEILARBOPPFOLGING_URL_PROPERTY, "http://localhost:8080/veilarboppfolging/api");
        setProperty(VEILARBAKTIVITET_URL_PROPERTY, "http://localhost:8080/veilarbaktivitet/api");
        setProperty(VEILARBVEILEDER_URL_PROPERTY, "http://localhost:8080/veilarbveileder/api");

        loadTestConfigFromProperties();

        setupTestContext();

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

    /**
     * Støtte for utvikler-lokale properties som ikke sjekkes inn i git
     * Bruker default test-properties dersom "veilarbportefolje-local.properties" ikke eksisterer
     */
    private static void loadTestConfigFromProperties() {
        try {
            SystemProperties.setFrom("veilarbportefolje-local.properties");
        } catch (Exception e) {
            SystemProperties.setFrom("veilarbportefolje.properties");
        }
    }

    private static DbCredentials createDbCredentials() {
        return (getProperty("database.url") == null) ? FasitUtils.getDbCredentials(APPLICATION_NAME) :
                new DbCredentials()
                        .setUrl(getProperty("database.url"))
                        .setUsername(getProperty("database.brukernavn", "sa"))
                        .setPassword(getProperty("database.passord", ""));
    }

}
