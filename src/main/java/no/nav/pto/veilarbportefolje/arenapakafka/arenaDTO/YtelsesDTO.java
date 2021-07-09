package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class YtelsesDTO extends GoldenGateDTO<YtelsesInnhold> {
    YtelsesInnhold before;
    YtelsesInnhold after;
}