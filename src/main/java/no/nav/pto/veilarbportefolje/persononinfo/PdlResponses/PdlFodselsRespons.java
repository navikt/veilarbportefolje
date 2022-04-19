package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.client.utils.graphql.GraphqlResponse;

import java.util.List;

public class PdlFodselsRespons extends GraphqlResponse<PdlFodselsRespons.HentFodselsResponseData> {
    @Data
    @Accessors(chain = true)
    public static class HentFodselsResponseData {
        private HentPersonDataResponsData hentPerson;

        @Data
        @Accessors(chain = true)
        public static class HentPersonDataResponsData {
            private List<Foedsel> foedsel;

            @Data
            @Accessors(chain = true)
            public static class Foedsel {
                private String foedselsdato;
            }
        }
    }
}
