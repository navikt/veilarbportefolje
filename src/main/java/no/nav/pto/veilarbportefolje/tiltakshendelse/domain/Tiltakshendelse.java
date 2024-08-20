package no.nav.pto.veilarbportefolje.tiltakshendelse.domain;

import no.nav.common.types.identer.Fnr;

import java.time.LocalDateTime;
import java.util.UUID;

public record Tiltakshendelse(
        UUID id,
        LocalDateTime opprettet,
        String tekst,
        String lenke,
        Tiltakstype tiltakstype,
        Fnr fnr) {
}
