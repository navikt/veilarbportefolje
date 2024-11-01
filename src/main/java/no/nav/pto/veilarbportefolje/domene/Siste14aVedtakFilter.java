package no.nav.pto.veilarbportefolje.domene;

import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;

public enum Siste14aVedtakFilter {
    HAR_14A_VEDTAK,
    HAR_IKKE_14A_VEDTAK;

    public static boolean contains(String value) {
        try {
            SisteEndringsKategori.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
