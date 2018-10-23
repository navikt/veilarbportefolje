package no.nav.fo.veilarbportefolje.exception;

import no.nav.fo.veilarbportefolje.domene.AktoerId;

import static java.lang.String.format;

public class FantIkkeFnrException extends RuntimeException {
    public FantIkkeFnrException(String aktoer) {
        super(format("Fant ikke fnr for aktoerId: %S", aktoer));
    }

    public FantIkkeFnrException(AktoerId aktoerId) {
        super(format("Fant ikke fnr for aktoerId: %S", aktoerId));
    }
}
