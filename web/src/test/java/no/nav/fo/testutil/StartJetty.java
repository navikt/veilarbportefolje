package no.nav.fo.testutil;

import lombok.SneakyThrows;
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
import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;


public class StartJetty {

    public static void main(String[] args) {
        Jetty jetty = nyJetty(null, 8765);
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    public static Jetty nyJetty(String contextPath, int jettyPort) {
        setupProperties();
        setupDataSource();
        setProperty(SubjectHandler.SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());
        setProperty(MILJO_PROPERTY_NAME, "t");
        return Jetty.usingWar()
                .at(contextPath)
                .port(jettyPort)
                .overrideWebXml()
                .disableAnnotationScanning()
                .buildJetty();
    }

    @SneakyThrows
    private static void setupDataSource() {
        SingleConnectionDataSource dataSource;
        dataSource = setupInMemoryDatabase();
        new Resource(DatabaseConfig.JNDI_NAME, dataSource);
    }

    private static void setupProperties() {
        System.setProperty("APP_LOG_HOME", new File("target").getAbsolutePath());
        System.setProperty("application.name", "veilarbportefolje");
        SystemProperties.setFrom("veilarbportefolje.properties");
    }

}
