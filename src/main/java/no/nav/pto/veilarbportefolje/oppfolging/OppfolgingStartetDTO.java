package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Value;
import no.nav.common.types.identer.AktorId;

import java.time.ZonedDateTime;

@Value
public class OppfolgingStartetDTO {
    AktorId aktorId;
    ZonedDateTime oppfolgingStartet;
}
