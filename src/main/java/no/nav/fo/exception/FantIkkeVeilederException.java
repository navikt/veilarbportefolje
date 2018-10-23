package no.nav.fo.exception;

import static java.lang.String.format;

public class FantIkkeVeilederException extends RuntimeException {
    public FantIkkeVeilederException(String aktoerId) {
        super(format("Fant ikke veileder for aktoerId: %S", aktoerId));
    }
}
