package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class TiltakDTO extends GoldenGateDTO<TiltakInnhold> {
    TiltakInnhold before;
    TiltakInnhold after;
}