package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype;

import java.time.LocalDate;

public record AapKelvinForBruker(
        LocalDate vedtaksdatoTilOgMed,
        String rettighetstype
) {
    public static AapKelvinForBruker of(LocalDate vedtaksdatoTilOgMed, AapRettighetstype rettighetstype) {
        if (vedtaksdatoTilOgMed != null || rettighetstype != null) {
            return new AapKelvinForBruker(vedtaksdatoTilOgMed, AapRettighetstype.Companion.tilFrontendtekst(rettighetstype));
        } else {
            return null;
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AapKelvinForBruker {
    }
}
