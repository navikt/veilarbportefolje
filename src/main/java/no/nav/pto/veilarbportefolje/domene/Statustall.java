package no.nav.pto.veilarbportefolje.domene;

import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse;

public record Statustall(
        long totalt,
        long ufordelteBrukere,
        long trengerVurdering,
        long nyeBrukereForVeileder,
        long inaktiveBrukere,
        long venterPaSvarFraNAV,
        long venterPaSvarFraBruker,
        long iavtaltAktivitet,
        long iAktivitet,
        long ikkeIavtaltAktivitet,
        long utlopteAktiviteter,
        long minArbeidsliste,
        long erSykmeldtMedArbeidsgiver,
        long moterMedNAVIdag,
        long underVurdering,
        long minArbeidslisteBla,
        long minArbeidslisteLilla,
        long minArbeidslisteGronn,
        long minArbeidslisteGul
) {
    public static Statustall of(StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets buckets, boolean vedtakstottePilotErPa) {
        return new Statustall(
                buckets.getTotalt().getDoc_count(),
                buckets.getUfordelteBrukere().getDoc_count(),
                buckets.getTrengerVurdering().getDoc_count(),
                buckets.getNyeBrukereForVeileder().getDoc_count(),
                buckets.getInaktiveBrukere().getDoc_count(),
                buckets.getVenterPaSvarFraNAV().getDoc_count(),
                buckets.getVenterPaSvarFraBruker().getDoc_count(),
                buckets.getIavtaltAktivitet().getDoc_count(),
                buckets.getIAktivitet().getDoc_count(),
                buckets.getIkkeIavtaltAktivitet().getDoc_count(),
                buckets.getUtlopteAktiviteter().getDoc_count(),
                buckets.getMinArbeidsliste().getDoc_count(),
                buckets.getErSykmeldtMedArbeidsgiver().getDoc_count(),
                buckets.getMoterMedNAVIdag().getDoc_count(),
                buckets.getMinArbeidslisteBla().getDoc_count(),
                buckets.getMinArbeidslisteLilla().getDoc_count(),
                buckets.getMinArbeidslisteGronn().getDoc_count(),
                buckets.getMinArbeidslisteGul().getDoc_count(),
                vedtakstottePilotErPa ? buckets.getUnderVurdering().getDoc_count() : 0
        );
    }
}