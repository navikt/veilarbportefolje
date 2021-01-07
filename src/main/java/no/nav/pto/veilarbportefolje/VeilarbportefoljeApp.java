package no.nav.pto.veilarbportefolje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;


@SpringBootApplication
@ServletComponentScan
public class VeilarbportefoljeApp {
    public static void main(String... args) {
        SpringApplication.run(VeilarbportefoljeApp.class, args);
    }
}
