package no.nav.fo.veilarbportefolje.krr;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KrrKontaktInfoDTO {
    private String personident;
    private boolean reservert;
}
