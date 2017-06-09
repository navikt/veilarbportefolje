package no.nav.fo.exception;

import static java.lang.String.format;

public class FantIkkeFnrException extends RuntimeException {
    public FantIkkeFnrException(String personident) {
        super(format("Finner ikke bruker med fnr %s i arena", personident));
    }
}
