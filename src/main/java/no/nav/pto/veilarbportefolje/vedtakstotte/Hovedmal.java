package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Hovedmal {
    SKAFFE_ARBEID,
    BEHOLDE_ARBEID,
    OKE_DELTAKELSE;

    public static boolean contains(Hovedmal value) {
        try {
            Hovedmal.valueOf(value.name());
            return true;
        } catch (Exception e) {
            log.warn("Kunne ikke instanstiere enum Hovedmal - fikk ugyldig verdi.");
            return false;
        }
    }
}
