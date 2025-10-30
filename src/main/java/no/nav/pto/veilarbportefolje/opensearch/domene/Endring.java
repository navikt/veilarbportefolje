package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Endring {
    public String aktivtetId;
    public String tidspunkt;
    public String er_sett;
}
