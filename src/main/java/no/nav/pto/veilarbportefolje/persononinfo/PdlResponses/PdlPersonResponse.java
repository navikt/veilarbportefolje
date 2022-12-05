package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.client.utils.graphql.GraphqlResponse;

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
            private List<Foedsel> foedsel;
            private List<Kjoenn> kjoenn;
            private List<Doedsfall> doedsfall;
            private List<Statsborgerskap> statsborgerskap;
            private List<Bostedsadresse> bostedsadresse;
            private List<TilrettelagtKommunikasjon> tilrettelagtKommunikasjon;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Kjoenn {
            private String kjoenn;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Foedsel {
            private String foedselsdato;
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
        public static class InnflyttingTilNorge {
            private String fraflyttingsland;
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
            private String angittFlyttedato;
            private Vegadresse vegadresse;
            private UtenlandskAdresse utenlandskAdresse;
            private UkjentBosted ukjentBosted;
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
        public static class Metadata {
            private boolean historisk;
            private PdlMaster master;
            private List<Endringer> endringer;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Endringer {
            private String registrert;
        }
    }

    public enum PdlMaster {
        PDL(1),
        FREG(2),
        UVIST(3);

        public final int prioritet;

        PdlMaster(int i) {
            prioritet = i;
        }

        @JsonCreator
        public static PdlMaster fromString(String string) {
            try {
                return PdlMaster.valueOf(string);
            } catch (IllegalArgumentException e) {
                return UVIST;
            }
        }
    }
}
