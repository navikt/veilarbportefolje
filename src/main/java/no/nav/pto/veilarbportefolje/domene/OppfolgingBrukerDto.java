package no.nav.pto.veilarbportefolje.domene;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class OppfolgingBrukerDto {
    private final String fnr;
    private final String personId;
}
