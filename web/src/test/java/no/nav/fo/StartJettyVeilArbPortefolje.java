package no.nav.fo;

import no.nav.brukerdialog.security.context.JettySubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.apache.geronimo.components.jaspi.AuthConfigFactoryImpl;


import javax.security.auth.message.config.AuthConfigFactory;
import java.security.Security;

import static java.lang.System.setProperty;
import static no.nav.fo.config.LocalJndiContextConfig.setupJndiLocalContext;
import static no.nav.modig.lang.collections.FactoryUtils.gotKeypress;
import static no.nav.modig.lang.collections.RunnableUtils.first;
import static no.nav.modig.lang.collections.RunnableUtils.waitFor;

public class StartJettyVeilArbPortefolje {

    public static void main(String[] args) throws Exception {
        SystemProperties.setFrom("veilarbportefolje.properties");
        setupJndiLocalContext();
        System.setProperty("develop-local", "true");
        setProperty("no.nav.modig.core.context.subjectHandlerImplementationClass", JettySubjectHandler.class.getName());
        System.setProperty("org.apache.geronimo.jaspic.configurationFile", "src/test/resources/jaspiconf.xml");
        Security.setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getCanonicalName());

//        final BrokerService broker = new BrokerService();
//        broker.getSystemUsage().getTempUsage().setLimit(100 * 1024 * 1024 * 100);
//        broker.getSystemUsage().getStoreUsage().setLimit(100 * 1024 * 1024 * 100);
//        broker.addConnector("tcp://localhost:61616");
//        broker.start();


        //Må ha https for csrf-token
        final Jetty jetty = Jetty.usingWar()
                .at("veilarbportefolje")
                .sslPort(9594)
                .port(9595)
                .overrideWebXml()
                .configureForJaspic()
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
