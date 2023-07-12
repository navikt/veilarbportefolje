package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

public enum PdlMaster {
    PDL(1),
    FREG(2),
    UVIST(3);

    public final int prioritet;

    PdlMaster(int i) {
        prioritet = i;
    }

    @JsonCreator
    public static PdlMaster fromString(String string) {
        try {
            return PdlMaster.valueOf(string.toUpperCase());
        } catch (IllegalArgumentException e) {
            secureLog.warn("Pdl kilde: " + string);
            return UVIST;
        }
    }
}
