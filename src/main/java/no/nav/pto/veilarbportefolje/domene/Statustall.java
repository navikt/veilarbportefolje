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
    private long mineHuskelapper;

    public Statustall() {
        this.totalt = 0;
        this.ufordelteBrukere = 0;
        this.trengerVurdering = 0;
        this.nyeBrukereForVeileder = 0;
        this.inaktiveBrukere = 0;
        this.venterPaSvarFraNAV = 0;
        this.venterPaSvarFraBruker = 0;
        this.iavtaltAktivitet = 0;
        this.ikkeIavtaltAktivitet = 0;
        this.utlopteAktiviteter = 0;
        this.minArbeidsliste = 0;
        this.erSykmeldtMedArbeidsgiver = 0;
        this.moterMedNAVIdag = 0;
        this.minArbeidslisteBla = 0;
        this.minArbeidslisteLilla = 0;
        this.minArbeidslisteGronn = 0;
        this.minArbeidslisteGul = 0;
        this.underVurdering = 0;
        this.mineHuskelapper = 0;
    }

    public Statustall(StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets buckets, boolean vedtakstottePilotErPa) {
        this.totalt = buckets.getTotalt().getDoc_count();
        this.ufordelteBrukere = buckets.getUfordelteBrukere().getDoc_count();
        this.trengerVurdering = buckets.getTrengerVurdering().getDoc_count();
        this.nyeBrukereForVeileder = buckets.getNyeBrukereForVeileder().getDoc_count();
        this.inaktiveBrukere = buckets.getInaktiveBrukere().getDoc_count();
        this.venterPaSvarFraNAV = buckets.getVenterPaSvarFraNAV().getDoc_count();
        this.venterPaSvarFraBruker = buckets.getVenterPaSvarFraBruker().getDoc_count();
        this.iavtaltAktivitet = buckets.getIavtaltAktivitet().getDoc_count();
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
        this.mineHuskelapper = buckets.getMineHuskelapper().getDoc_count();
    }
}