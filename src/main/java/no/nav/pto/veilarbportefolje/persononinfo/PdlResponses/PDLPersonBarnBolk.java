package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.utils.graphql.GraphqlResponse;

import java.util.List;

@Data
@Slf4j
@Accessors(chain = true)
public class PDLPersonBarnBolk extends GraphqlResponse<PDLPersonBarnBolk.PdlBarnResponseData> {
    @Data
    @Accessors(chain = true)
    public static class PdlBarnResponseData {
        private List<PdlResponseBolk> hentPersonBolk;

        @Data
        @Accessors(chain = true)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PdlResponseBolk {
            private String ident;
            private PdlBarnResponse.PdlBarnResponseData.HentPersonResponsData person;
            private String code;
        }
    }

}
