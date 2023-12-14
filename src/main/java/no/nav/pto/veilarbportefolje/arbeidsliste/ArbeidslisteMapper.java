package no.nav.pto.veilarbportefolje.arbeidsliste;

public class ArbeidslisteMapper {

    private ArbeidslisteMapper() { /* no-op */ }

    public static Arbeidsliste.Kategori mapFraFargekategoriTilKategori(String kategoriFraFargekategoriTabell) {
        return switch (kategoriFraFargekategoriTabell) {
            case "FARGEKATEGORI_A" -> Arbeidsliste.Kategori.BLA;
            case "FARGEKATEGORI_B" -> Arbeidsliste.Kategori.GRONN;
            case "FARGEKATEGORI_C" -> Arbeidsliste.Kategori.GUL;
            case "FARGEKATEGORI_D" -> Arbeidsliste.Kategori.LILLA;
            default -> throw new RuntimeException("Ukjent kategori: " + kategoriFraFargekategoriTabell);
        };
    }
}