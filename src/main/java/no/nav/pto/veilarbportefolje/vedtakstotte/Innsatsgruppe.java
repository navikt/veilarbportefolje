package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;

@Slf4j
public enum Innsatsgruppe {
    STANDARD_INNSATS,
    SITUASJONSBESTEMT_INNSATS,
    SPESIELT_TILPASSET_INNSATS,
    GRADERT_VARIG_TILPASSET_INNSATS,
    VARIG_TILPASSET_INNSATS;

    public static boolean contains(Innsatsgruppe value) {
        try {
            Innsatsgruppe.valueOf(value.name());
            return true;
        } catch (Exception e) {
            log.warn("Kunne ikke instanstiere enum Innsatsgruppe - fikk ugyldig verdi.");
            return false;
        }
    }
}
