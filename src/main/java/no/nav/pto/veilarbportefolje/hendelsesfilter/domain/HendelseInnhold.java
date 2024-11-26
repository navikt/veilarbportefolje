package no.nav.pto.veilarbportefolje.hendelsesfilter.domain;

import java.time.LocalDateTime;

public record HendelseInnhold(
        String navn,
        LocalDateTime dato,
        String lenke,
        String detaljer
) {
}
