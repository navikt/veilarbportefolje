package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class GruppeAktivitetDTO extends GoldenGateDTO<GruppeAktivitetInnhold> {
    GruppeAktivitetInnhold before;
    GruppeAktivitetInnhold after;
}