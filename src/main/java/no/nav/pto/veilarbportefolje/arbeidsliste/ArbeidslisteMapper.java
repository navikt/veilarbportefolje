package no.nav.pto.veilarbportefolje.arbeidsliste;

import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi;

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

    public static Arbeidsliste.Kategori mapFraKategoriTilKategori(String kategoriFraArbeidslisteTabell) {
        return switch (kategoriFraArbeidslisteTabell) {
            case "BLA" -> Arbeidsliste.Kategori.BLA;
            case "GRONN" -> Arbeidsliste.Kategori.GRONN;
            case "GUL" -> Arbeidsliste.Kategori.GUL;
            case "LILLA" -> Arbeidsliste.Kategori.LILLA;
            default -> throw new RuntimeException("Ukjent kategori: " + kategoriFraArbeidslisteTabell);
        };
    }

    public static String mapTilFargekategoriVerdi(Arbeidsliste.Kategori kategori) {
        if (kategori == null) {
            return null;
        }
        return switch (kategori) {
            case BLA -> FargekategoriVerdi.BLA.verdi;
            case GRONN -> FargekategoriVerdi.GRONN.verdi;
            case GUL -> FargekategoriVerdi.GUL.verdi;
            case LILLA -> FargekategoriVerdi.LILLA.verdi;
        };
    }
}
