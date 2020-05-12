package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Builder;
import lombok.Value;
import no.nav.json.JsonUtils;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import oracle.sql.ZONEIDMAP;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@Value
@Builder
public class OppfolgingStatus {
    AktoerId aktoerId;
    String veileder;
    boolean oppfolging;
    boolean nyForVeileder;
    boolean manuell;
    ZonedDateTime endretTimestamp;
    ZonedDateTime startDato;
}


