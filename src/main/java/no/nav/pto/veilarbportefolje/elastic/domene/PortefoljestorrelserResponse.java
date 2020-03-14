package no.nav.pto.veilarbportefolje.elastic.domene;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PortefoljestorrelserResponse {
    Hits hits;
    PortefoljestorrelserAggregations aggregations;

    @Data
    public class PortefoljestorrelserAggregations {
        @JsonProperty("filter#portefoljestorrelser")
        PortefoljestorrelseFilter filter;

        @Data
        public class PortefoljestorrelseFilter {
            Long doc_count;
            @JsonProperty("sterms#portefoljestorrelser")
            PortefoljeStorrelser sterms;

            @Data
            public class PortefoljeStorrelser {
                List<Bucket> buckets;
            }
        }
    }

}
