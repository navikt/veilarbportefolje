package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record OppfolgingsbrukerEntity(
        String fodselsnr,
        String formidlingsgruppekode,
        ZonedDateTime iserv_fra_dato,
        String etternavn,
        String fornavn,
        String nav_kontor,
        String kvalifiseringsgruppekode,
        String rettighetsgruppekode,
        String hovedmaalkode,
        String sikkerhetstiltak_type_kode,
        String fr_kode,
        boolean sperret_ansatt,
        boolean er_doed,
        ZonedDateTime endret_dato
) {
}
