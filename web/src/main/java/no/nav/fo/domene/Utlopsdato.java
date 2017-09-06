package no.nav.fo.domene;

import no.nav.fo.exception.YtelseManglerTOMDatoException;
import no.nav.melding.virksomhet.loependeytelser.v1.AAPtellere;
import no.nav.melding.virksomhet.loependeytelser.v1.Dagpengetellere;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;

import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

public class Utlopsdato {
    public static LocalDateTime utlopsdato(LoependeVedtak loependeVedtak) {
        if (loependeVedtak.getVedtaksperiode() != null && loependeVedtak.getVedtaksperiode().getTom() != null) {
            return loependeVedtak.getVedtaksperiode().getTom().toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        }

        throw new YtelseManglerTOMDatoException(loependeVedtak);
    }
}
