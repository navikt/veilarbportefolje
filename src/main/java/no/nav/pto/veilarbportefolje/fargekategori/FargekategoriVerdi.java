package no.nav.pto.veilarbportefolje.fargekategori;

public enum FargekategoriVerdi {
    BLA("FARGEKATEGORI_A"),
    GRONN("FARGEKATEGORI_B"),
    GUL("FARGEKATEGORI_C"),
    LILLA("FARGEKATEGORI_D"),
    LIMEGRONN("FARGEKATEGORI_E"),
    ORANSJE("FARGEKATEGORI_F");

    public final String verdi;
    private FargekategoriVerdi(String verdi) {
        this.verdi = verdi;
    }

}
