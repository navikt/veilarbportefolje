package no.nav.pto.veilarbportefolje.elastic.domene;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Endring {
    private String aktivtetId;
    private String tidspunkt;
}
