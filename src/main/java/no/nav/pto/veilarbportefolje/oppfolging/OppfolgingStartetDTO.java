package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Value;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;

import java.time.ZonedDateTime;

@Value
public class OppfolgingStartetDTO {
    AktoerId aktorId;
    ZonedDateTime oppfolgingStartet;
}
