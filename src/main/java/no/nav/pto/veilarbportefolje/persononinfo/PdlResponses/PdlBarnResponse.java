package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.AdressebeskyttelseDto;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto.Metadata;

import java.util.List;

public class PdlBarnResponse extends GraphqlResponse<PdlBarnResponse.PdlBarnResponseData> {
    @Data
    @Accessors(chain = true)
    public static class PdlBarnResponseData {
        private HentPersonResponsData hentPerson;

        @Data
        @Accessors(chain = true)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class HentPersonResponsData {
            private List<Foedseldato> foedselsdato;
            private List<Doedsfall> doedsfall;
            private List<AdressebeskyttelseDto> adressebeskyttelse;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Foedseldato {
            private String foedselsdato;
            private Metadata metadata;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Doedsfall {
            private String doedsdato;
        }


    }


}
