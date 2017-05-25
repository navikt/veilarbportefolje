package no.nav.fo.exception;

import static java.lang.String.format;

public class FantIkkeFnrException extends RuntimeException {
    public FantIkkeFnrException(String personident) {
        super(format("Fant ikke fnr for person_id: %S", personident));
    }
}
