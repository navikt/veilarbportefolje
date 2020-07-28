import no.nav.common.cxf.StsSecurityConstants;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Import;

@EnableAutoConfiguration
@ServletComponentScan
@Import(ApplicationConfigTest.class)
public class VeilarbportefoljeTestApp {

    public static void main(String[] args) {
        System.setProperty(StsSecurityConstants.STS_URL_KEY, "");
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, "username");
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, "password");

        SpringApplication application = new SpringApplication(VeilarbportefoljeTestApp.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }
}
