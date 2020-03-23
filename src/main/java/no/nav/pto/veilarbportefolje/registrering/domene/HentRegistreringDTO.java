package no.nav.pto.veilarbportefolje.registrering.domene;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;

@Data
@AllArgsConstructor
public class HentRegistreringDTO {
    AktoerId aktoerId;
    Fnr fnr;
}
