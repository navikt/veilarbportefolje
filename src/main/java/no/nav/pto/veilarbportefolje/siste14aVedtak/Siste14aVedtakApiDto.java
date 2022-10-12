package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class Siste14aVedtakApiDto {
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    ZonedDateTime fattetDato;
    boolean fraArena;
}
