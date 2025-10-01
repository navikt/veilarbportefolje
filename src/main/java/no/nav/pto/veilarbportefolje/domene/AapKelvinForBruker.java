package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public record AapKelvinForBruker(
        LocalDate vedtaksdatoTilOgMed,
        String rettighetstype
) {
    public static AapKelvinForBruker of(LocalDate vedtaksdatoTilOgMed, String rettighetstype) {
        if(vedtaksdatoTilOgMed != null || rettighetstype != null){
            return new AapKelvinForBruker(vedtaksdatoTilOgMed, rettighetstype);
        } else {
            return null;
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AapKelvinForBruker {
    }
}
