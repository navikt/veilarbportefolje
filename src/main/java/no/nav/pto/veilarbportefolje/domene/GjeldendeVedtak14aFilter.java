package no.nav.pto.veilarbportefolje.domene;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum GjeldendeVedtak14aFilter {
    HAR_14A_VEDTAK,
    HAR_IKKE_14A_VEDTAK;

    public static boolean contains(String value) {
        try {
            GjeldendeVedtak14aFilter.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Kunne ikke instanstiere enum - fikk ugyldig verdi.");
            return false;
        }
    }
}
