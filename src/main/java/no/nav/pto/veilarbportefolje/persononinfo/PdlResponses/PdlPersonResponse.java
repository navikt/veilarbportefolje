package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.client.utils.graphql.GraphqlResponse;

import java.time.LocalDate;
import java.util.List;


public class PdlPersonResponse extends GraphqlResponse<PdlPersonResponse.PdlPersonResponseData> {
    @Data
    @Accessors(chain = true)
    public static class PdlPersonResponseData {
        private HentPersonResponsData hentPerson;

        @Data
        @Accessors(chain = true)
        public static class HentPersonResponsData {
            private List<Folkeregisteridentifikator> folkeregisteridentifikator;
            private List<Navn> navn;
            private List<Foedsel> foedsel;
            private List<Kjoenn> kjoenn;
            private List<Doedsfall> doedsfall;
        }

        @Data
        public static class Kjoenn {
            private String kjoenn;
        }

        @Data
        public static class Foedsel {
            private LocalDate foedselsdato;
        }

        @Data
        public static class Doedsfall {
            private LocalDate doedsdato;
        }

        @Data
        public static class Navn {
            private String fornavn;
            private String mellomnavn;
            private String etternavn;
        }

        @Data
        public static class Folkeregisteridentifikator {
            private String identifikasjonsnummer;
        }
    }
}
