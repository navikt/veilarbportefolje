package no.nav.fo;

import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;

import static no.nav.modig.testcertificates.TestCertificates.setupKeyAndTrustStore;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

public class StartJettyVeilArbPortefolje {

    public static void main(String[] args) {
        SystemProperties.setFrom("jetty-VeilArbPortefolje.properties");
        setupKeyAndTrustStore();

        //Må ha https for csrf-token
        final Jetty jetty = Jetty.usingWar()
                .at("Portefølje-serverside")
                .sslPort(9594)
                .port(9595)
                .overrideWebXml()
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }
}
