package no.nav.pto.veilarbportefolje.oppfolging.dto;

import lombok.Value;
import no.nav.common.types.identer.AktorId;

@Value
public class NyForVeilederDTO {
    AktorId aktorId;
    boolean nyForVeileder;
}
