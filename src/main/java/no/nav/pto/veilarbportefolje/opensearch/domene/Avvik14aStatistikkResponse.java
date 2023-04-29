package no.nav.pto.veilarbportefolje.opensearch.domene;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Avvik14aStatistikkResponse {
    Hits hits;
    Avvik14aStatistikkAggregation aggregations;

    @Data
    public static class Avvik14aStatistikkAggregation {
        @JsonProperty("filters#avvik14astatistikk")
        Avvik14aStatistikkFilter filters;

        @Data
        public static class Avvik14aStatistikkFilter {
            Avvik14aStatistikkBuckets buckets;

            @Data
            public static class Avvik14aStatistikkBuckets {
                Bucket innsatsgruppeUlik;
                Bucket hovedmaalUlik;
                Bucket innsatsgruppeOgHovedmaalUlik;
                Bucket innsatsgruppeManglerINyKilde;
            }
        }
    }
}
