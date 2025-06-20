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
                Bucket trengerOppfolgingsvedtak;
                Bucket nyeBrukereForVeileder;
                Bucket inaktiveBrukere;
                Bucket venterPaSvarFraNAV;
                Bucket venterPaSvarFraBruker;
                Bucket iavtaltAktivitet;
                Bucket ikkeIavtaltAktivitet;
                Bucket utlopteAktiviteter;
                Bucket erSykmeldtMedArbeidsgiver;
                Bucket moterMedNAVIdag;
                Bucket underVurdering;
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

    /**
     * Disse verdiene korresponderer 1-til-1 med {@link StatustallAggregation.StatustallFilter.StatustallBuckets} og
     * {@link no.nav.pto.veilarbportefolje.domene.Statustall} sine klassefelter. Brukes når vi gjør en keyed aggregation query mot OpenSearch.
     * <br>
     * <br>
     * Dvs. en {@link StatustallAggregationKey} er det vi ber OpenSearch om å bruke som navn for et gitt aggregation query,
     * og {@link StatustallAggregation.StatustallFilter.StatustallBuckets} er de samme navnene men deserialisert til klassefelt med samme navn.
     * Disse mappes videre til korresponderende klassefelt i {@link no.nav.pto.veilarbportefolje.domene.Statustall}.
     * <br>
     * <br>
     * Dersom vi ønsker en ny aggregation query (les: et nytt statustall) så bør man lage en {@link StatustallAggregationKey}
     * som brukes i et faktisk query og oppdatere {@link StatustallAggregation.StatustallFilter.StatustallBuckets} med et
     * tilsvarende nytt klassefelt, samt mappe feltet i konstruktøren {@link no.nav.pto.veilarbportefolje.domene.Statustall}.
     */
    public enum StatustallAggregationKey {
        TOTALT("totalt"),
        UFORDELTE_BRUKERE("ufordelteBrukere"),
        TRENGER_OPPFOLGINGSVEDTAK("trengerOppfolgingsvedtak"),
        NYE_BRUKERE_FOR_VEILEDER("nyeBrukereForVeileder"),
        INAKTIVE_BRUKERE("inaktiveBrukere"),
        VENTER_PA_SVAR_FRA_NAV("venterPaSvarFraNAV"),
        VENTER_PA_SVAR_FRA_BRUKER("venterPaSvarFraBruker"),
        I_AVTALT_AKTIVITET("iavtaltAktivitet"),
        IKKE_I_AVTALT_AKTIVITET("ikkeIavtaltAktivitet"),
        UTLOPTE_AKTIVITETER("utlopteAktiviteter"),
        ER_SYKMELDT_MED_ARBEIDSGIVER("erSykmeldtMedArbeidsgiver"),
        MOTER_MED_NAV_I_DAG("moterMedNAVIdag"),
        UNDER_VURDERING("underVurdering"),
        MINE_HUSKELAPPER("mineHuskelapper"),
        FARGEKATEGORI_A("fargekategoriA"),
        FARGEKATEGORI_B("fargekategoriB"),
        FARGEKATEGORI_C("fargekategoriC"),
        FARGEKATEGORI_D("fargekategoriD"),
        FARGEKATEGORI_E("fargekategoriE"),
        FARGEKATEGORI_F("fargekategoriF"),
        FARGEKATEGORI_INGEN_KATEGORI("fargekategoriIngenKategori"),
        TILTAKSHENDELSER("tiltakshendelser"),
        UTGATTE_VARSEL("utgatteVarsel");

        public final String key;

        StatustallAggregationKey(String key) {
            this.key = key;
        }
    }
}
