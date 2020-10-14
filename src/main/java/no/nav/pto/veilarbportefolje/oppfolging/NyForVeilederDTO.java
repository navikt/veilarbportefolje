package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Value;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;

@Value
public class NyForVeilederDTO {
    AktoerId aktorId;
    boolean nyForVeileder;
}
