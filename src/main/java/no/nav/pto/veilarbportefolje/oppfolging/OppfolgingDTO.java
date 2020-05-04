package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Builder;
import lombok.Value;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.VeilederId;

import java.time.OffsetDateTime;

@Value
@Builder
public class OppfolgingDTO {
    AktoerId aktoerId;
    VeilederId veileder;
    boolean oppfolging;
    boolean nyForVeileder;
    boolean manuell;
    OffsetDateTime endretTimestamp;
    OffsetDateTime startDato;
}

