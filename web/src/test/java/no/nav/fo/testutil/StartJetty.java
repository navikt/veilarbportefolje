package no.nav.fo.testutil;

import lombok.SneakyThrows;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.fo.config.DatabaseConfig;
import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.modig.core.context.SubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.eclipse.jetty.plus.jndi.Resource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.File;

import static java.lang.System.setProperty;
import static no.nav.apiapp.rest.ExceptionMapper.MILJO_PROPERTY_NAME;
import static no.nav.fo.StartJettyVeilArbPortefolje.APPLICATION_NAME;
import static no.nav.fo.StartJettyVeilArbPortefolje.TEST_ENV;
import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;


public class StartJetty {

    static SingleConnectionDataSource ds;

    public static void main(String[] args) {
        Jetty jetty = nyJetty(null, 8765);
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    static Jetty nyJetty(String contextPath, int jettyPort) {
        setupProperties();
        setupDataSource();
        setProperty(SubjectHandler.SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        setProperty(MILJO_PROPERTY_NAME, "t");

        Jetty.JettyBuilder builder = Jetty.usingWar()
                .at(contextPath)
                .port(jettyPort)
                .overrideWebXml()
                .disableAnnotationScanning();

        DevelopmentSecurity.ISSOSecurityConfig issoSecurityConfig =
                new DevelopmentSecurity.ISSOSecurityConfig(APPLICATION_NAME, TEST_ENV);

        return DevelopmentSecurity
                .setupISSO(builder, issoSecurityConfig)
                .configureForJaspic()
                .buildJetty();
    }

    @SneakyThrows
    private static void setupDataSource() {
        ds = setupInMemoryDatabase();
        new Resource(DatabaseConfig.JNDI_NAME, ds);
    }

    private static void setupProperties() {
        System.setProperty("APP_LOG_HOME", new File("target").getAbsolutePath());
        System.setProperty("application.name", APPLICATION_NAME);
        SystemProperties.setFrom("veilarbportefolje.properties");
    }

}
