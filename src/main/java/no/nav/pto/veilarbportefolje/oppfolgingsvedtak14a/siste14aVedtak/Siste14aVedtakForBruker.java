package no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;

import java.time.ZonedDateTime;

@Data
@Builder
@Accessors(chain = true)
@AllArgsConstructor
public class Siste14aVedtakForBruker {
    AktorId aktorId;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    ZonedDateTime fattetDato;
    boolean fraArena;

    public AktorId getAktorId() {
        return aktorId;
    }

    public Innsatsgruppe getInnsatsgruppe() {
        return innsatsgruppe;
    }

    public Hovedmal getHovedmal() {
        return hovedmal;
    }

    public ZonedDateTime getFattetDato() {
        return fattetDato;
    }

    public boolean isFraArena() {
        return fraArena;
    }

    public static Siste14aVedtakForBruker fraKafkaDto(Siste14aVedtakKafkaDto dto) {
        return new Siste14aVedtakForBruker(dto.aktorId, dto.innsatsgruppe, dto.hovedmal, dto.fattetDato, dto.fraArena);
    }

    public static Siste14aVedtakForBruker fraApiDto(Siste14aVedtakApiDto dto, AktorId aktorId) {
        return new Siste14aVedtakForBruker(aktorId, dto.innsatsgruppe, dto.hovedmal, dto.fattetDato, dto.fraArena);
    }
}
