package no.nav.fo;

import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;


import static no.nav.fo.config.LocalJndiContextConfig.setupJndiLocalContext;
import static no.nav.modig.lang.collections.FactoryUtils.gotKeypress;
import static no.nav.modig.lang.collections.RunnableUtils.first;
import static no.nav.modig.lang.collections.RunnableUtils.waitFor;
import static no.nav.modig.testcertificates.TestCertificates.setupKeyAndTrustStore;

public class StartJettyVeilArbPortefolje {

    public static void main(String[] args) throws Exception {
        SystemProperties.setFrom("veilarbportefolje.properties");
        setupKeyAndTrustStore();
        setupJndiLocalContext();

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
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

}
