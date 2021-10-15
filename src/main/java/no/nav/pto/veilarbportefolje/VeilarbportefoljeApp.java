package no.nav.pto.veilarbportefolje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import javax.annotation.PostConstruct;
import java.util.TimeZone;


@SpringBootApplication
@ServletComponentScan
public class VeilarbportefoljeApp {
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv("TZ")));
    }

    public static void main(String... args) {
        SpringApplication.run(VeilarbportefoljeApp.class, args);
    }
}
