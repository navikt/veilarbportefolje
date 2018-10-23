package no.nav.fo.veilarbportefolje.exception;


import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;

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
