package no.nav.pto.veilarbportefolje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;


@SpringBootApplication
@ServletComponentScan
public class VeilarbportefoljeApp {

    public static void main(String... args) {
        System.setProperty("oppfolging.feed.brukertilgang", "srvveilarboppfolging");
        System.setProperty("javax.xml.bind.JAXBContextFactory", "com.sun.xml.bind.v2.JAXBContextFactory");

        SpringApplication.run(VeilarbportefoljeApp.class, args);
    }

}
