package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record OppfolgingsbrukerEntity(
        String fodselsnr,
        String formidlingsgruppekode,
        ZonedDateTime iserv_fra_dato,
        String nav_kontor,
        String kvalifiseringsgruppekode,
        String rettighetsgruppekode,
        String hovedmaalkode,
        boolean sperret_ansatt,
        ZonedDateTime endret_dato
) {
}
