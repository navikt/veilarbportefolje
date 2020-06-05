import no.nav.common.leaderelection.ElectorMock;
import no.nav.testconfig.ApiAppTest;

import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public class MainTest {

    private static final String PORT = "9595";

    public static void main(String[] args) {

        setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());

        ElectorMock.start();
        Main.main(PORT);
    }
}
