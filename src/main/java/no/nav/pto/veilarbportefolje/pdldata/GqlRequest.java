package no.nav.pto.veilarbportefolje.pdldata;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GqlRequest<V> {
    String query;
    V variables;
}
