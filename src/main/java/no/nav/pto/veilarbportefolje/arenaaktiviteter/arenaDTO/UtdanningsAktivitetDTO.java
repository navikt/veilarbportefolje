package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UtdanningsAktivitetDTO extends GoldenGateDTO {
    UtdanningsAktivitetInnhold before;
    UtdanningsAktivitetInnhold after;
}