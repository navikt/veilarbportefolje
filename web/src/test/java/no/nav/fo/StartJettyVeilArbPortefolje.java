package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.fo.config.DatabaseConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.eclipse.jetty.plus.jndi.Resource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static java.lang.System.getProperty;
import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.fo.config.LocalJndiContextConfig.setupOracleDataSource;
import static no.nav.fo.config.SecurityTestConfig.setupLdap;
import static no.nav.modig.lang.collections.FactoryUtils.gotKeypress;
import static no.nav.modig.lang.collections.RunnableUtils.first;
import static no.nav.modig.lang.collections.RunnableUtils.waitFor;

public class StartJettyVeilArbPortefolje {

    public static void main(String[] args) throws Exception {
        SystemProperties.setFrom("veilarbportefolje.properties");
        System.setProperty("develop-local", "true");
        System.setProperty("environment.class", "lokalt");

        SingleConnectionDataSource dataSource;
        if (Boolean.parseBoolean(getProperty("lokal.database"))) {
            dataSource = setupInMemoryDatabase();
        } else {
            dataSource = setupOracleDataSource();
        }

//        InternbrukerSubjectHandler.setVeilederIdent("!!CHANGE ME!!");
//        InternbrukerSubjectHandler.setServicebruker("!!CHANGE ME!!");
//        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());

//        System.setProperty("org.apache.geronimo.jaspic.configurationFile", "src/test/resources/jaspiconf.xml");
//        Security.setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());


        setupLdap();


        // TODO slett når common-jetty registerer datasource fornuftig
        new Resource(DatabaseConfig.JNDI_NAME, dataSource);

        //Må ha https for csrf-token
        Jetty jetty = DevelopmentSecurity.setupISSO(Jetty.usingWar()
                .at("veilarbportefolje")
                .sslPort(9594)
                .port(9595)
                .addDatasource(dataSource, DatabaseConfig.JNDI_NAME)
                .overrideWebXml(), new ISSOSecurityConfig("veilarbportefolje", "t5"))
                .buildJetty();

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
