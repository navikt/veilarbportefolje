package no.nav.pto.veilarbportefolje.ensligforsorger.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class EnsligForsorgerRequestParam {
    String personIdent;
}
