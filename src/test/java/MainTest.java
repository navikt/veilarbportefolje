import no.nav.common.leaderelection.ElectorMock;
import no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants;
import no.nav.testconfig.ApiAppTest;

import java.util.Optional;

import static java.lang.System.setProperty;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig.VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.*;
import static no.nav.pto.veilarbportefolje.config.DatabaseConfig.*;
import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.HSQL_URL;
import static no.nav.sbl.dialogarena.common.abac.pep.service.AbacServiceConfig.ABAC_ENDPOINT_URL_PROPERTY_NAME;
import static no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants.*;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class MainTest {

    private static final String PORT = "9595";

    public static void main(String[] args) {

        setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());

        ElectorMock.start();
        Main.main(PORT);
    }
}
