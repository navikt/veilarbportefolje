package no.nav.fo;

import no.nav.modig.security.loginmodule.DummyRole;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.eclipse.jetty.jaas.JAASLoginService;

import java.io.File;

import static no.nav.modig.core.test.FilesAndDirs.TEST_RESOURCES;
import static no.nav.modig.core.test.FilesAndDirs.WEBAPP_SOURCE;
import static no.nav.modig.lang.collections.FactoryUtils.gotKeypress;
import static no.nav.modig.lang.collections.RunnableUtils.first;
import static no.nav.modig.lang.collections.RunnableUtils.waitFor;
import static no.nav.modig.testcertificates.TestCertificates.setupKeyAndTrustStore;

public class StartJettyVeilArbPortefolje {

    public static void main(String[] args) {
        SystemProperties.setFrom("jetty-local.properties");
        setupKeyAndTrustStore();

        //Må ha https for csrf-token
        final Jetty jetty = Jetty.usingWar(WEBAPP_SOURCE)
                .at("veilarbportefolje")
                .sslPort(9594)
                .port(9595)
                .overrideWebXml(new File(TEST_RESOURCES,"override-web.xml" ))
                .withLoginService(createLoginService())
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }



    public static JAASLoginService createLoginService() {
        JAASLoginService jaasLoginService = new JAASLoginService("Simple Login Realm");
        jaasLoginService.setLoginModuleName("simplelogin");
        jaasLoginService.setRoleClassNames(new String[]{DummyRole.class.getName()});
        return jaasLoginService;
    }


}
