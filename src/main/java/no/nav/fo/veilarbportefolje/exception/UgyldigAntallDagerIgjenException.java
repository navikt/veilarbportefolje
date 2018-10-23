package no.nav.fo.veilarbportefolje.exception;

import static java.lang.String.format;

public class UgyldigAntallDagerIgjenException extends RuntimeException {
    public UgyldigAntallDagerIgjenException(int antallDagerIgjen) {
        super(format("Ugyldig antallDagerIgjenUnntak: %s", antallDagerIgjen));
    }
}
