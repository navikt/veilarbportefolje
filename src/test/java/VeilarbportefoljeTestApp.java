import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.TimeZone;

@EnableAutoConfiguration
@ServletComponentScan
@Import(ApplicationConfigTest.class)
public class VeilarbportefoljeTestApp {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(Optional.ofNullable(System.getenv("TZ")).orElse("Europe/Oslo")));
        SpringApplication application = new SpringApplication(VeilarbportefoljeTestApp.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }
}
