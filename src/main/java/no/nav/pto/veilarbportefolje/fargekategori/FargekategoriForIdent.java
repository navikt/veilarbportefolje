package no.nav.pto.veilarbportefolje.fargekategori;

import java.time.LocalDateTime;

public record FargekategoriForIdent(
        String fnr,
        String verdi,
        String enhetId,
        LocalDateTime sistEndret
) {}
