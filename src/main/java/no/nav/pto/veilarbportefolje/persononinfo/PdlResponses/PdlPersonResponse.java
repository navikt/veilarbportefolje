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
        public static class Folkeregisteridentifikator {
            private String identifikasjonsnummer;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Metadata {
            private boolean historisk;
            private PdlMaster master;
        }
    }

    // NB: java "ordinal" (deklarasjons posisjon) er tatt i bruk for kilde prioritering
    public enum PdlMaster {
        PDL,
        FREG,
        UVIST;

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
