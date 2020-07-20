import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;


@EnableAutoConfiguration
@Import(ApplicationConfigTest.class)
public class VeilarbportefoljeTestApp {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VeilarbportefoljeTestApp.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }
}
