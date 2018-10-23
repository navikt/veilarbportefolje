package no.nav.fo.veilarbportefolje.exception;

import no.nav.fo.veilarbportefolje.domene.AktoerId;

import static java.lang.String.format;

public class FantIkkePersonIdException extends RuntimeException {
    public FantIkkePersonIdException(String aktoerId) {
        super(format("Fant ikke personid for aktoerId: %S", aktoerId));
    }

    public FantIkkePersonIdException(AktoerId aktoerId) {
        super(format("Fant ikke personid for aktoerId: %S", aktoerId));
    }
}
