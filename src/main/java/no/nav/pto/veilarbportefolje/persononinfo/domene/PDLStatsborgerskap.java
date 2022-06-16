package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Statsborgerskap;

import java.time.LocalDate;

@Data
@Slf4j
@Accessors(chain = true)
@RequiredArgsConstructor
public class PDLStatsborgerskap {
    private final String land;
    private final LocalDate gyldigFraOgMed;
    private final LocalDate gyldigTilOgMed;

    public Statsborgerskap toStatsborgerskap() {
        return new Statsborgerskap(this.land, this.gyldigFraOgMed, this.gyldigTilOgMed);
    }
}
