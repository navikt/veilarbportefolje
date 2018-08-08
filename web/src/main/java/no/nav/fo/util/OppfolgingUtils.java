package no.nav.fo.util;


import java.sql.ResultSet;
import java.sql.SQLException;

public class OppfolgingUtils {
    public static boolean erBrukerUnderOppfolging(String formidlingsgruppekode, String servicegruppekode, boolean oppfolgingsbruker) {
        return oppfolgingsbruker ||
                UnderOppfolgingRegler.erUnderOppfolging(formidlingsgruppekode, servicegruppekode);
    }

    static boolean isNyForEnhet(ResultSet rs) throws SQLException {
        String veilder = rs.getString("veilederident");
        return veilder == null || veilder.isEmpty();
    }

    static boolean trengerVurdering(String formidlingsgruppekode, String kvalifiseringsgruppekode) {
        if ("ISERV".equals(formidlingsgruppekode)) {
            return false;
        }
        return "IVURD".equals(kvalifiseringsgruppekode) || "BKART".equals(kvalifiseringsgruppekode);
    }
}
