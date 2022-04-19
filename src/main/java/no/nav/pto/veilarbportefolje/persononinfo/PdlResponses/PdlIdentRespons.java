package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.client.utils.graphql.GraphqlResponse;

import java.util.List;


public class PdlIdentRespons extends GraphqlResponse<PdlIdentRespons.HentIdenterResponseData> {
    @Data
    @Accessors(chain = true)
    public static class HentIdenterResponseData {
        private HentIdenterResponsData hentIdenter;

        @Data
        @Accessors(chain = true)
        public static class HentIdenterResponsData {
            private List<PDLIdent> identer;
        }
    }
}
