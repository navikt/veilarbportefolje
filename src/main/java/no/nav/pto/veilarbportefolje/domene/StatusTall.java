package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets;

@Data
@Accessors(chain = true)
public class StatusTall {
    public long totalt;
    public long ufordelteBrukere;
    public long trengerVurdering;
    public long nyeBrukereForVeileder;
    public long inaktiveBrukere;
    public long venterPaSvarFraNAV;
    public long venterPaSvarFraBruker;
    public long iavtaltAktivitet;
    public long iAktivitet;
    public long ikkeIavtaltAktivitet;
    public long utlopteAktiviteter;
    public long minArbeidsliste;
    public long erSykmeldtMedArbeidsgiver;
    public long moterMedNAVIdag;
    public long underVurdering;
    public long minArbeidslisteBla;
    public long minArbeidslisteLilla;
    public long minArbeidslisteGronn;
    public long minArbeidslisteGul;
    public StatusTallUtvidet kode6Brukere;
    public StatusTallUtvidet kode7Brukere;
    public StatusTallUtvidet egenAnsattBrukere;

    public StatusTall(StatustallBuckets buckets, boolean vedtakstottePilotErPa) {
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
        this.kode6Brukere = new StatusTallUtvidet(buckets.getKode6BrukereUfordelt().getDoc_count(), buckets.getKode6BrukereVenterPaSvarFraNav().getDoc_count());
        this.kode7Brukere = new StatusTallUtvidet(buckets.getKode7BrukereUfordelt().getDoc_count(), buckets.getKode7BrukereVenterPaSvarFraNav().getDoc_count());
        this.egenAnsattBrukere = new StatusTallUtvidet(buckets.getEgenAnsattBrukereUfordelt().getDoc_count(), buckets.getEgenAnsattBrukereVenterPaSvarFraNav().getDoc_count());
    }
}
