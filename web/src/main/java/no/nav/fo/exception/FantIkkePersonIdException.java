package no.nav.fo.exception;

import static java.lang.String.format;

public class FantIkkePersonIdException extends RuntimeException {
    public FantIkkePersonIdException(String fnr) {
        super(format("Fant ikke person_id for fnr: %S", fnr));
    }
}
