package no.nav.fo.config;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.Portefolje;
import org.fluttercode.datafactory.impl.DataFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Configuration
public class PortefoljeMock {

    @Bean
    public Portefolje portefoljeMock() {
        List<Bruker> brukere = createBrukerListe(1000);

        return new Portefolje().withBrukere(brukere);
    }

    private List<Bruker> createBrukerListe(int antallBrukere) {
        List<Bruker> brukerliste =  new ArrayList<>();
        DataFactory df = new DataFactory();

        Date minDate = new Date(-1893459600000L);
        Date maxDate = new Date(915145200000L);

        for(int i=0; i < antallBrukere; i++) {
            Bruker bruker = new Bruker()
                                .withVeilederId("X123456")
                                .withFornavn(df.getFirstName())
                                .withEtternavn(df.getLastName())
                                .withFnr(dateToFnr(df.getDateBetween(minDate, maxDate)));

            switch(i % 23) {
                case 1: bruker
                        .addSikkerhetstiltak("Sikkerhetstiltak 1")
                        .addSikkerhetstiltak("Sikkerhetstiltak 2");
                        break;
                case 4: bruker.addSikkerhetstiltak("Sikkerhetstiltak 1")
                                .withDiskresjonskode("6");
                        break;
                case 12: bruker.erEgenAnsatt();
                        break;
                case 17: bruker.withDiskresjonskode("6");
                        break;
                case 22: bruker.withDiskresjonskode("7");
                        break;
            }

            brukerliste.add(bruker);
        }

        return brukerliste;
    }

    private String dateToFnr(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMuu");
        return localDate.format(formatter) + "00000";
    }

}
