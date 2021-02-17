package no.nav.pto.veilarbportefolje.pdldata;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class PdlDto {
    public PdlPerson hentPerson;

    @Data
    public static class PdlPerson {
        List<Navn> navn;
        List<Foedsel> foedsel;
        List<Kjoenn> kjoenn;
        List<Doedsfall> doedsfall;
        List<Sikkerhetstiltak> sikkerhetstiltak;
        List<Adressebeskyttelse> adressebeskyttelse;
    }

    @Data
    public static class Navn {
        String fornavn;
        String mellomnavn;
        String etternavn;
        String forkortetNavn;
    }

    @Data
    public static class Adressebeskyttelse {
        private String gradering;
    }

    @Data
    public static class Doedsfall {
        private String doedsdato;
    }

    @Data
    public static class Foedsel {
        private String foedselsdato;
    }

    @Data
    public static class Kjoenn {
        private String kjoenn;
    }

    @Data
    public static class Sikkerhetstiltak {
        private String tiltakstype;
        private String beskrivelse;
    }

}
