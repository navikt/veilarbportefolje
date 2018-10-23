package no.nav.fo.exception;

import no.nav.fo.domene.Fnr;

import static java.lang.String.format;

public class FantIkkeAktoerIdException extends RuntimeException {
    public FantIkkeAktoerIdException(Fnr fnr) {
        super(format("Fant ikke aktoerId for fnr: %S", fnr));
    }
}
