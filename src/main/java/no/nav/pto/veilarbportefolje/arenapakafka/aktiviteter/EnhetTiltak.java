package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;


import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class EnhetTiltak {
    Map<String, String> tiltak;
}
