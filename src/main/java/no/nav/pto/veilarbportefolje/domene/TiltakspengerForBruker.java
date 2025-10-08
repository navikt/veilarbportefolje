package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet;

import java.time.LocalDate;

public record TiltakspengerForBruker(
        LocalDate vedtaksdatoTilOgMed,
        String rettighet
) {
    public static TiltakspengerForBruker of(LocalDate vedtaksdatoTilOgMed, TiltakspengerRettighet rettighet) {
        if (vedtaksdatoTilOgMed != null || rettighet != null) {
            return new TiltakspengerForBruker(vedtaksdatoTilOgMed, TiltakspengerRettighet.Companion.tilFrontendtekst(rettighet));
        } else {
            return null;
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TiltakspengerForBruker {
    }
}
