package no.nav.fo;

import no.nav.brukerdialog.security.Constants;
import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.util.Util;
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
        System.setProperty("lokal.database", "true");

        SingleConnectionDataSource dataSource;
        if (Boolean.parseBoolean(getProperty("lokal.database"))) {
            dataSource = setupInMemoryDatabase();
        } else {
            dataSource = setupOracleDataSource();
        }

        if(Boolean.parseBoolean(getProperty("use.jaspic"))) {
            InternbrukerSubjectHandler.setVeilederIdent("!!CHANGE ME!!");
            InternbrukerSubjectHandler.setServicebruker("!!CHANGE ME!!");
            setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());
        }else {
            System.setProperty("org.apache.geronimo.jaspic.configurationFile", "src/test/resources/jaspiconf.xml");
            Security.setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());
        }

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
                .configureForJaspic()
                .buildJetty();


        Util.setProperty("oidc-redirect.url", "https://app-t6.adeo.no/veilarbportefoljeflatefs/tjenester/login");
        System.setProperty("environment.class", "lokalt");

        //Fra versjon 3.0.0 av oidc-security bruker vi nå fullstendig url. Setter denne slik for nå, at den overskriver ressurs fra fasit.
        System.setProperty(Constants.OIDC_REDIRECT_URL,"https://app-t5.adeo.no/veilarbportefoljeflatefs/tjenester/login");

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
