package no.nav.pto.veilarbportefolje.sistelest;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class SistLestKafkaMelding {
    AktorId aktorId;
    VeilederId veilederId;
    ZonedDateTime harLestTidspunkt;
}
