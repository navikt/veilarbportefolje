package no.nav.fo.domene;

import no.nav.fo.exception.YtelseManglerTOMDatoException;
import no.nav.melding.virksomhet.loependeytelser.v1.AAPtellere;
import no.nav.melding.virksomhet.loependeytelser.v1.Dagpengetellere;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;

import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

public class Utlopsdato {
    private static final String DAGPENGER = "DAGP";

    public static LocalDateTime utlopsdato(LocalDateTime now, LoependeVedtak loependeVedtak) {
        if (loependeVedtak.getVedtaksperiode() != null && loependeVedtak.getVedtaksperiode().getTom() != null) {
            return loependeVedtak.getVedtaksperiode().getTom().toGregorianCalendar().toZonedDateTime().toLocalDateTime();
        }

        if (!DAGPENGER.equals(loependeVedtak.getSakstypeKode())) {
            throw new YtelseManglerTOMDatoException(loependeVedtak);
        }

        return utlopsdatoUtregning(now, loependeVedtak.getDagpengetellere());
    }

    public static LocalDateTime utlopsdatoUtregning(LocalDateTime now, Dagpengetellere dagpengetellere) {
        return utlopsdatoUtregning(now, nullcheck(dagpengetellere.getAntallUkerIgjen()).intValue(), nullcheck(dagpengetellere.getAntallDagerIgjen()).intValue());
    }

    public static LocalDateTime utlopsdatoUtregning(LocalDateTime now, AAPtellere aaptellere) {
        return utlopsdatoUtregning(now, nullcheck(aaptellere.getAntallUkerIgjen()).intValue(), nullcheck(aaptellere.getAntallDagerIgjen()).intValue());
    }

    public static LocalDateTime utlopsdatoUtregning(LocalDateTime now, int antallUkerIgjen, int antallDagerIgjen) {
        LocalDateTime utlopsdato = now
                .minusDays(1)
                .plusWeeks(antallUkerIgjen)
                .plusDays(antallDagerIgjen);

        while (utlopsdato.getDayOfWeek() == DayOfWeek.SATURDAY || utlopsdato.getDayOfWeek() == DayOfWeek.SUNDAY) {
            utlopsdato = utlopsdato.plusDays(1);
        }

        return utlopsdato;
    }

    private static BigInteger nullcheck(BigInteger bi) {
        if (bi == null) {
            return BigInteger.ZERO;
        }
        return bi;
    }
}
