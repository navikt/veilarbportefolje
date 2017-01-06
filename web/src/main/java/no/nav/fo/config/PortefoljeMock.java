package no.nav.fo.config;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Portefolje;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class PortefoljeMock {

    @Bean
    public Portefolje portefoljeMock(){
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(new Bruker().withFnr("***REMOVED***")
                .withFornavn("Arnfinn")
                .withEtternavn("Dalstrøm"));
        brukere.add(new Bruker().withFnr("***REMOVED***")
                .withFornavn("Jens")
                .withEtternavn("Jensen"));
        brukere.add(new Bruker().withFnr("***REMOVED***")
                .withFornavn("Donald")
                .withEtternavn("Duck"));
        return new Portefolje().withBrukere(brukere);
    }

}
