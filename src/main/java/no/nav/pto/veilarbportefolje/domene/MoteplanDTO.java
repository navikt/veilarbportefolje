package no.nav.pto.veilarbportefolje.domene;

import java.time.Duration;

/** Møteplan-data som sendes til frontend. */
public record MoteplanDTO(Motedeltaker deltaker, String dato, int varighetMinutter, boolean avtaltMedNav) {
    public static MoteplanDTO of(Moteplan moteplan) {
        Long varighet = Duration.between(moteplan.starttidspunkt(), moteplan.sluttidspunkt()).toMinutes();

        return new MoteplanDTO(moteplan.deltaker(), moteplan.dato(), varighet.intValue(), moteplan.avtaltMedNav());
    }
}
