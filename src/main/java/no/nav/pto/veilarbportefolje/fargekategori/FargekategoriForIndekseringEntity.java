package no.nav.pto.veilarbportefolje.fargekategori;

import java.time.LocalDateTime;

public record FargekategoriForIndekseringEntity(
        String fnr,
        String verdi,
        String enhetId,
        LocalDateTime sistEndret
) {}
