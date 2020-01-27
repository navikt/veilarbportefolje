package no.nav.pto.veilarbportefolje.domene;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class EnhetTiltak {
    Map<String, String> tiltak;
}
