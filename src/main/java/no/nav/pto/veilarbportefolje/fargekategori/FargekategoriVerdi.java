package no.nav.pto.veilarbportefolje.fargekategori;

public enum FargekategoriVerdi {
    BLA("FARGEKATEGORI_A"),
    GRONN("FARGEKATEGORI_B"),
    GUL("FARGEKATEGORI_C"),
    LILLA("FARGEKATEGORI_D"),
    LIMEGRONN("FARGEKATEGORI_E"),
    ORANSJE("FARGEKATEGORI_F"),

    // TODO: Hjelpe Jackson til å deserialisere FargekategoriVerdi basert på verdi
    // slik at vi slipper å ha en enum som heter FARGEKATEGORI_A her
    FARGEKATEGORI_A("FARGEKATEGORI_A");

    public final String verdi;
    private FargekategoriVerdi(String verdi) {
        this.verdi = verdi;
    }

}
