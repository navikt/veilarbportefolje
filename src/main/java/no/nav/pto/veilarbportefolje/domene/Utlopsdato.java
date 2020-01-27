package no.nav.pto.veilarbportefolje.domene;

import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;

import java.time.LocalDateTime;

public class Utlopsdato {
    public static LocalDateTime utlopsdato(LoependeVedtak loependeVedtak) {
        if (loependeVedtak.getVedtaksperiode() != null && loependeVedtak.getVedtaksperiode().getTom() != null) {
            return loependeVedtak.getVedtaksperiode().getTom().toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        }
        throw new RuntimeException("Ytelse mangler til og med-dato");
    }
}
