package no.nav.fo.exception;

import static java.lang.String.format;

public class UgyldigFnrException extends RuntimeException {
    public UgyldigFnrException(String fnr) {
        super(format("Fødselsnummer er på ugyldig format: %s", fnr));
    }
}
