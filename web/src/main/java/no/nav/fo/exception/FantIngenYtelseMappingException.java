package no.nav.fo.exception;

import no.nav.fo.loependeytelser.LoependeVedtak;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class FantIngenYtelseMappingException extends RuntimeException {
    public FantIngenYtelseMappingException(LoependeVedtak loependeVedtak) {
        super("Fant ingen ytelsemapping for vedtak. " + ReflectionToStringBuilder.toString(loependeVedtak));
    }
}
