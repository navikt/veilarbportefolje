package no.nav.pto.veilarbportefolje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import static javax.xml.bind.JAXBContext.JAXB_CONTEXT_FACTORY;


@SpringBootApplication
@ServletComponentScan
public class VeilarbportefoljeApp {

    public static void main(String... args) {
        System.setProperty("oppfolging.feed.brukertilgang", "srvveilarboppfolging");
        System.setProperty(JAXB_CONTEXT_FACTORY, "com.sun.xml.bind.v2.JAXBContextFactory");

        SpringApplication.run(VeilarbportefoljeApp.class, args);
    }

}
