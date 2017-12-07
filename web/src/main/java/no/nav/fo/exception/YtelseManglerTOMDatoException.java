package no.nav.fo.exception;

import no.nav.fo.loependeytelser.LoependeVedtak;

import static java.lang.String.format;

public class YtelseManglerTOMDatoException extends RuntimeException {
    public YtelseManglerTOMDatoException(LoependeVedtak loependeVedtak) {
        super(format(
                "Fant ingen TOM-dato for vedtak for %s sakstype %s rettighetstype %s",
                loependeVedtak.getPersonident(),
                loependeVedtak.getSakstypeKode(),
                loependeVedtak.getRettighetstypeKode()
        ));
    }
}
