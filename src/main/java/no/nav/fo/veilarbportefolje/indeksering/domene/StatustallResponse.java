package no.nav.fo.veilarbportefolje.indeksering.domene;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StatustallResponse {
    Hits hits;
    StatustallAggregation aggregations;

    @Data
    public class StatustallAggregation {
        @JsonProperty("filters#statustall")
        StatustallFilter filters;

        @Data
        public class StatustallFilter {
            StatustallBuckets buckets;

            @Data
            public class StatustallBuckets {
                Bucket totalt;
                Bucket nyeBrukere;
                Bucket ufordelteBrukere;
                Bucket trengerVurdering;
                Bucket nyeBrukereForVeileder;
                Bucket inaktiveBrukere;
                Bucket venterPaSvarFraNAV;
                Bucket venterPaSvarFraBruker;
                Bucket iavtaltAktivitet;
                Bucket ikkeIavtaltAktivitet;
                Bucket utlopteAktiviteter;
                Bucket minArbeidsliste;
                Bucket erSykmeldtMedArbeidsgiver;
                Bucket moterMedNAVIdag;
            }
        }
    }
}
