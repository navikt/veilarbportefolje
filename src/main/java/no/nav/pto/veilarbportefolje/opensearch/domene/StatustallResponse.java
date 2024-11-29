package no.nav.pto.veilarbportefolje.opensearch.domene;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StatustallResponse {
    Hits hits;
    StatustallAggregation aggregations;

    @Data
    public static class StatustallAggregation {
        @JsonProperty("filters#statustall")
        StatustallFilter filters;

        @Data
        public static class StatustallFilter {
            StatustallBuckets buckets;

            @Data
            public static class StatustallBuckets {
                Bucket totalt;
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
                Bucket underVurdering;
                Bucket minArbeidslisteBla;
                Bucket minArbeidslisteLilla;
                Bucket minArbeidslisteGronn;
                Bucket minArbeidslisteGul;
                Bucket adressebeskyttelseEllerSkjermingTotalt;
                Bucket adressebeskyttelseEllerSkjermingUfordelte;
                Bucket adressebeskyttelseEllerSkjermingVenterPaSvarFraNAV;
                Bucket mineHuskelapper;
                Bucket fargekategoriA;
                Bucket fargekategoriB;
                Bucket fargekategoriC;
                Bucket fargekategoriD;
                Bucket fargekategoriE;
                Bucket fargekategoriF;
                Bucket fargekategoriIngenKategori;
                Bucket tiltakshendelser;
                Bucket utgatteVarsel;
            }
        }
    }
}
