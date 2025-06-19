package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse;

@Data
@Accessors(chain = true)
public class Statustall {
    private long totalt;
    private long ufordelteBrukere;
    private long trengerOppfolgingsvedtak;
    private long nyeBrukereForVeileder;
    private long inaktiveBrukere;
    private long venterPaSvarFraNAV;
    private long venterPaSvarFraBruker;
    private long iavtaltAktivitet;
    private long ikkeIavtaltAktivitet;
    private long utlopteAktiviteter;
    private long erSykmeldtMedArbeidsgiver;
    private long moterMedNAVIdag;
    private long underVurdering;
    private long mineHuskelapper;
    private long fargekategoriA;
    private long fargekategoriB;
    private long fargekategoriC;
    private long fargekategoriD;
    private long fargekategoriE;
    private long fargekategoriF;
    private long fargekategoriIngenKategori;
    private long tiltakshendelser;
    private long utgatteVarsel;

    public Statustall() {
        this.totalt = 0;
        this.ufordelteBrukere = 0;
        this.trengerOppfolgingsvedtak = 0;
        this.nyeBrukereForVeileder = 0;
        this.inaktiveBrukere = 0;
        this.venterPaSvarFraNAV = 0;
        this.venterPaSvarFraBruker = 0;
        this.iavtaltAktivitet = 0;
        this.ikkeIavtaltAktivitet = 0;
        this.utlopteAktiviteter = 0;
        this.erSykmeldtMedArbeidsgiver = 0;
        this.moterMedNAVIdag = 0;
        this.underVurdering = 0;
        this.mineHuskelapper = 0;
        this.fargekategoriA = 0;
        this.fargekategoriB = 0;
        this.fargekategoriC = 0;
        this.fargekategoriD = 0;
        this.fargekategoriE = 0;
        this.fargekategoriF = 0;
        this.fargekategoriIngenKategori = 0;
        this.tiltakshendelser = 0;
        this.utgatteVarsel = 0;
    }

    public Statustall(StatustallResponse.StatustallAggregation.StatustallFilter.StatustallBuckets buckets) {
        this.totalt = buckets.getTotalt().getDoc_count();
        this.ufordelteBrukere = buckets.getUfordelteBrukere().getDoc_count();
        this.trengerOppfolgingsvedtak = buckets.getTrengerOppfolgingsvedtak().getDoc_count();
        this.nyeBrukereForVeileder = buckets.getNyeBrukereForVeileder().getDoc_count();
        this.inaktiveBrukere = buckets.getInaktiveBrukere().getDoc_count();
        this.venterPaSvarFraNAV = buckets.getVenterPaSvarFraNAV().getDoc_count();
        this.venterPaSvarFraBruker = buckets.getVenterPaSvarFraBruker().getDoc_count();
        this.iavtaltAktivitet = buckets.getIavtaltAktivitet().getDoc_count();
        this.ikkeIavtaltAktivitet = buckets.getIkkeIavtaltAktivitet().getDoc_count();
        this.utlopteAktiviteter = buckets.getUtlopteAktiviteter().getDoc_count();
        this.erSykmeldtMedArbeidsgiver = buckets.getErSykmeldtMedArbeidsgiver().getDoc_count();
        this.moterMedNAVIdag = buckets.getMoterMedNAVIdag().getDoc_count();
        this.underVurdering = buckets.getUnderVurdering().getDoc_count();
        this.mineHuskelapper = buckets.getMineHuskelapper().getDoc_count();
        this.fargekategoriA = buckets.getFargekategoriA().getDoc_count();
        this.fargekategoriB = buckets.getFargekategoriB().getDoc_count();
        this.fargekategoriC = buckets.getFargekategoriC().getDoc_count();
        this.fargekategoriD = buckets.getFargekategoriD().getDoc_count();
        this.fargekategoriE = buckets.getFargekategoriE().getDoc_count();
        this.fargekategoriF = buckets.getFargekategoriF().getDoc_count();
        this.fargekategoriIngenKategori = buckets.getFargekategoriIngenKategori().getDoc_count();
        this.tiltakshendelser = buckets.getTiltakshendelser().getDoc_count();
        this.utgatteVarsel = buckets.getUtgatteVarsel().getDoc_count();
    }
}
