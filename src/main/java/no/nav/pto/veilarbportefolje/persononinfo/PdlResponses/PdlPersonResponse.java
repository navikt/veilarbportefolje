package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.AdressebeskyttelseDto;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Metadata;

import java.util.List;

public class PdlPersonResponse extends GraphqlResponse<PdlPersonResponse.PdlPersonResponseData> {
    @Data
    @Accessors(chain = true)
    public static class PdlPersonResponseData {
        private HentPersonResponsData hentPerson;

        @Data
        @Accessors(chain = true)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class HentPersonResponsData {
            private List<Folkeregisteridentifikator> folkeregisteridentifikator;
            private List<Navn> navn;
            private List<Foedseldato> foedselsdato;
            private List<Foedested> foedested;
            private List<Kjoenn> kjoenn;
            private List<Doedsfall> doedsfall;
            private List<Statsborgerskap> statsborgerskap;
            private List<Bostedsadresse> bostedsadresse;
            private List<TilrettelagtKommunikasjon> tilrettelagtKommunikasjon;
            private List<AdressebeskyttelseDto> adressebeskyttelse;
            private List<Sikkerhetstiltak> sikkerhetstiltak;
            private List<Foreldreansvar> foreldreansvar;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Kjoenn {
            private String kjoenn;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Foedseldato {
            private String foedselsdato;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Foedested {
            private String foedeland;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Doedsfall {
            private String doedsdato;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Navn {
            private String fornavn;
            private String mellomnavn;
            private String etternavn;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Statsborgerskap {
            private String land;
            private String gyldigFraOgMed;
            private String gyldigTilOgMed;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TilrettelagtKommunikasjon {
            private Spraak talespraaktolk;
            private Spraak tegnspraaktolk;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Folkeregisteridentifikator {
            private String identifikasjonsnummer;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Bostedsadresse {
            private Vegadresse vegadresse;
            private UtenlandskAdresse utenlandskAdresse;
            private UkjentBosted ukjentBosted;
            private Metadata metadata;
        }

        @Data
        public static class Sikkerhetstiltak {
            private String tiltakstype;
            private String beskrivelse;
            private String gyldigFraOgMed;
            private String gyldigTilOgMed;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Vegadresse {
            private final String kommunenummer;
            private final String bydelsnummer;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UtenlandskAdresse {
            private String landkode;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class UkjentBosted {
            private String bostedskommune;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Spraak {
            private String spraak;
        }


        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Foreldreansvar {
            private String ansvarssubjekt;
            private Metadata metadata;
        }
    }
}
