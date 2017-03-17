package no.nav.fo.exception;

import static java.lang.String.format;

public class FantIkkePersonIdException extends RuntimeException {
    public FantIkkePersonIdException(String personident) {
        super(format("Fant ikke fnr for person_id: %S", personident));
    }
}
