package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class UtdanningsAktivitetDTO extends GoldenGateDTO<UtdanningsAktivitetInnhold> {
    UtdanningsAktivitetInnhold before;
    UtdanningsAktivitetInnhold after;
}