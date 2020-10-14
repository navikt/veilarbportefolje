package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Value;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

@Value
public class VeilederTilordnetDTO {
    AktoerId aktorId;
    VeilederId veilederId;
}
