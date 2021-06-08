package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TiltakDTO extends GoldenGateDTO {
    TiltakInnhold before;
    TiltakInnhold after;
}