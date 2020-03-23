package no.nav.pto.veilarbportefolje.registrering.domene;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class BrukerRegistreringWrapper {
    String type;
    OrdinaerBrukerRegistrering registrering;
}