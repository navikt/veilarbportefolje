package no.nav.fo;

import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.TestEnvironment;
import no.nav.fo.config.DatabaseConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;
import org.eclipse.jetty.plus.jndi.Resource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.security.auth.message.config.AuthConfigFactory;
import java.security.Security;

import static java.lang.System.getProperty;
import static no.nav.dialogarena.config.util.Util.setProperty;
import static no.nav.fo.config.LocalJndiContextConfig.setServiceUserCredentials;
import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.fo.config.LocalJndiContextConfig.setupOracleDataSource;
import static no.nav.modig.lang.collections.FactoryUtils.gotKeypress;
import static no.nav.modig.lang.collections.RunnableUtils.first;
import static no.nav.modig.lang.collections.RunnableUtils.waitFor;

public class StartJettyVeilArbPortefolje {

    public static final TestEnvironment TEST_ENV = TestEnvironment.T6;
    public static final String APPLICATION_NAME = "veilarbportefolje";
    public static final String SERVICE_USER_NAME = "srvveilarbportefolje";

    public static void main(String[] args) throws Exception {
        SystemProperties.setFrom("veilarbportefolje.properties");

        SingleConnectionDataSource dataSource;
        if (Boolean.parseBoolean(getProperty("lokal.database"))) {
            dataSource = setupInMemoryDatabase();
        } else {
            dataSource = setupOracleDataSource(FasitUtils.getDbCredentials(TEST_ENV, APPLICATION_NAME));
        }

        if(Boolean.parseBoolean(getProperty("no-security"))) {
            InternbrukerSubjectHandler.setVeilederIdent("!!CHANGE ME!!");
            InternbrukerSubjectHandler.setServicebruker("!!CHANGE ME!!");
            setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());
        }else {
            System.setProperty("org.apache.geronimo.jaspic.configurationFile", "src/test/resources/jaspiconf.xml");
            Security.setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());
        }

        setServiceUserCredentials(FasitUtils.getServiceUser(SERVICE_USER_NAME, APPLICATION_NAME, TEST_ENV.toString()));

        // TODO slett når common-jetty registerer datasource fornuftig
        new Resource(DatabaseConfig.JNDI_NAME, dataSource);

        //Må ha https for csrf-token
        Jetty jetty = DevelopmentSecurity.setupISSO(Jetty.usingWar()
                .at("veilarbportefolje")
                .sslPort(9594)
                .port(9595)
                .addDatasource(dataSource, DatabaseConfig.JNDI_NAME)
                .overrideWebXml(), new ISSOSecurityConfig(APPLICATION_NAME, TEST_ENV.toString()))
                .configureForJaspic()
                .buildJetty();

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }
}
