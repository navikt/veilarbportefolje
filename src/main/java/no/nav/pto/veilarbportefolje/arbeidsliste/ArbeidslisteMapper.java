package no.nav.pto.veilarbportefolje.arbeidsliste;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSLISTE.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

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
            case BLA -> FargekategoriVerdi.FARGEKATEGORI_A.name();
            case GRONN -> FargekategoriVerdi.FARGEKATEGORI_B.name();
            case GUL -> FargekategoriVerdi.FARGEKATEGORI_C.name();
            case LILLA -> FargekategoriVerdi.FARGEKATEGORI_D.name();
        };
    }

    @SneakyThrows
    public static Arbeidsliste arbeidslisteMapper(ResultSet rs, int row) {
        return new Arbeidsliste(
                VeilederId.of(rs.getString(SIST_ENDRET_AV_VEILEDERIDENT)),
                toZonedDateTime(rs.getTimestamp(ENDRINGSTIDSPUNKT)),
                rs.getString(OVERSKRIFT),
                rs.getString(KOMMENTAR),
                toZonedDateTime(rs.getTimestamp(FRIST)),
                getResolvedKategori(rs)
        ).setAktoerid(rs.getString(AKTOERID));
    }

    @SneakyThrows
    public static Arbeidsliste arbeidslisteMapper(Map<String, Object> rs) {
        return new Arbeidsliste(
                VeilederId.of((String) rs.get(SIST_ENDRET_AV_VEILEDERIDENT)),
                toZonedDateTime((Timestamp) rs.get(ENDRINGSTIDSPUNKT)),
                (String) rs.get(OVERSKRIFT),
                (String) rs.get(KOMMENTAR),
                toZonedDateTime((Timestamp) rs.get(FRIST)),
                getResolvedKategori(rs)
        ).setAktoerid((String) rs.get(AKTOERID));
    }

    @Nullable
    public static Arbeidsliste.Kategori getResolvedKategori(Map<String, Object> rs) {
        String kategoriFraFargekategoriTabell = (String) rs.get("VERDI");
        String kategoriFraArbeidslisteTabell = (String) rs.get("KATEGORI");

        if (kategoriFraFargekategoriTabell != null) {
            return mapFraFargekategoriTilKategori(kategoriFraFargekategoriTabell);
        }

        if (kategoriFraArbeidslisteTabell != null) {
            return mapFraKategoriTilKategori(kategoriFraArbeidslisteTabell);
        }

        return null;
    }

    @Nullable
    @SneakyThrows
    public static Arbeidsliste.Kategori getResolvedKategori(ResultSet rs) {
        String kategoriFraFargekategoriTabell = rs.getString("VERDI");
        String kategoriFraArbeidslisteTabell = rs.getString("KATEGORI");

        if (kategoriFraFargekategoriTabell != null) {
            return mapFraFargekategoriTilKategori(kategoriFraFargekategoriTabell);
        }

        if (kategoriFraArbeidslisteTabell != null) {
            return mapFraKategoriTilKategori(kategoriFraArbeidslisteTabell);
        }

        return null;
    }
}
