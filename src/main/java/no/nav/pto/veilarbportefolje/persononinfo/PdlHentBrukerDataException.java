package no.nav.pto.veilarbportefolje.persononinfo;

import no.nav.common.types.identer.Fnr;

public class PdlHentBrukerDataException extends RuntimeException {
    private final Fnr fnr;

    public PdlHentBrukerDataException(Fnr fnr, Throwable cause) {
        super("Kunne ikke hente brukerdata fra PDL", cause);
        this.fnr = fnr;
    }

    public Fnr getFnr() {
        return fnr;
    }
}
