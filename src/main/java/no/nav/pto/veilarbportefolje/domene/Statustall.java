package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse;

@Data
@Accessors(chain = true)
public class Statustall {
    private long totalt;
    private long ufordelteBrukere;
    private long trengerVurdering;
    private long nyeBrukereForVeileder;
    private long inaktiveBrukere;
    private long venterPaSvarFraNAV;
    private long venterPaSvarFraBruker;
    private long iavtaltAktivitet;
    private long iAktivitet;
    private long ikkeIavtaltAktivitet;
    private long utlopteAktiviteter;
    private long minArbeidsliste;
    private long erSykmeldtMedArbeidsgiver;
    private long moterMedNAVIdag;
    private long underVurdering;
    private long minArbeidslisteBla;
    private long minArbeidslisteLilla;
    private long minArbeidslisteGronn;
    private long minArbeidslisteGul;

    public Statustall(StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets buckets, boolean vedtakstottePilotErPa) {
        this.totalt = buckets.getTotalt().getDoc_count();
        this.ufordelteBrukere = buckets.getUfordelteBrukere().getDoc_count();
        this.trengerVurdering = buckets.getTrengerVurdering().getDoc_count();
        this.nyeBrukereForVeileder = buckets.getNyeBrukereForVeileder().getDoc_count();
        this.inaktiveBrukere = buckets.getInaktiveBrukere().getDoc_count();
        this.venterPaSvarFraNAV = buckets.getVenterPaSvarFraNAV().getDoc_count();
        this.venterPaSvarFraBruker = buckets.getVenterPaSvarFraBruker().getDoc_count();
        this.iavtaltAktivitet = buckets.getIavtaltAktivitet().getDoc_count();
        this.iAktivitet = buckets.getIAktivitet().getDoc_count();
        this.ikkeIavtaltAktivitet = buckets.getIkkeIavtaltAktivitet().getDoc_count();
        this.utlopteAktiviteter = buckets.getUtlopteAktiviteter().getDoc_count();
        this.minArbeidsliste = buckets.getMinArbeidsliste().getDoc_count();
        this.erSykmeldtMedArbeidsgiver = buckets.getErSykmeldtMedArbeidsgiver().getDoc_count();
        this.moterMedNAVIdag = buckets.getMoterMedNAVIdag().getDoc_count();
        this.minArbeidslisteBla = buckets.getMinArbeidslisteBla().getDoc_count();
        this.minArbeidslisteLilla = buckets.getMinArbeidslisteLilla().getDoc_count();
        this.minArbeidslisteGronn = buckets.getMinArbeidslisteGronn().getDoc_count();
        this.minArbeidslisteGul = buckets.getMinArbeidslisteGul().getDoc_count();
        this.underVurdering = vedtakstottePilotErPa ? buckets.getUnderVurdering().getDoc_count() : 0;
    }
}