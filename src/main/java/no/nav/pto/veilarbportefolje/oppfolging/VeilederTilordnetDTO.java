package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Value;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

@Value
public class VeilederTilordnetDTO {
    AktorId aktorId;
    VeilederId veilederId;
}
