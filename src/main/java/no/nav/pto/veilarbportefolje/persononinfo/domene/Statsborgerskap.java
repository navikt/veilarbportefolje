package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Data
@Slf4j
@Accessors(chain = true)
@RequiredArgsConstructor
public class Statsborgerskap {
    private final String land;
    private final LocalDate gyldigFraOgMed;
    private final LocalDate gyldigTilOgMed;
}
