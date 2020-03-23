package no.nav.pto.veilarbportefolje.registrering.domene;

import lombok.AllArgsConstructor;
import lombok.Value;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;

@Value
@AllArgsConstructor
public class HentRegistreringDTO {
    AktoerId aktoerId;
    Fnr fnr;
}
