package no.nav.pto.veilarbportefolje.pdldata;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.client.utils.graphql.GraphqlResponse;

import java.util.List;

public class PdlFodselsRespons  extends GraphqlResponse<PdlFodselsRespons.HentFodselsResponseData>{
    @Accessors(chain = true)
    @Data
    public static class HentFodselsResponseData {
        private HentPersonDataResponsData hentPerson;
        @Accessors(chain = true)
        @Data
        public static class HentPersonDataResponsData {
            private List<Foedsel> foedsel;
            @Accessors(chain = true)
            @Data
            public static class Foedsel {
                private String foedselsdato;
            }
        }
    }
}
