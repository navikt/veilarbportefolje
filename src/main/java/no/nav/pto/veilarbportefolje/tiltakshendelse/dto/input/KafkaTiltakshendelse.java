package no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Avsender;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;

import java.time.LocalDateTime;
import java.util.UUID;

public record KafkaTiltakshendelse(
        UUID id,
        Boolean aktiv,
        LocalDateTime opprettet,
        String tekst,
        String lenke,
        Tiltakstype tiltakstype,
        Fnr fnr,
        Avsender avsender
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public KafkaTiltakshendelse {
    }

    public static Tiltakshendelse mapTilTiltakshendelse(KafkaTiltakshendelse hendelse) {
        return new Tiltakshendelse(hendelse.id, hendelse.opprettet, hendelse.tekst, hendelse.lenke, hendelse.tiltakstype, hendelse.fnr);
    }
}
