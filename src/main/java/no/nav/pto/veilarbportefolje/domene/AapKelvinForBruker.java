package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.aap.domene.Rettighetstype;

import java.time.LocalDate;

public record AapKelvinForBruker(
        LocalDate vedtaksdatoTilOgMed,
        String rettighetstype
) {
    public static AapKelvinForBruker of(LocalDate vedtaksdatoTilOgMed, Rettighetstype rettighetstype) {
        if (vedtaksdatoTilOgMed != null || rettighetstype != null) {
            return new AapKelvinForBruker(vedtaksdatoTilOgMed, Rettighetstype.Companion.tilFrontendtekst(rettighetstype));
        } else {
            return null;
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AapKelvinForBruker {
    }
}
