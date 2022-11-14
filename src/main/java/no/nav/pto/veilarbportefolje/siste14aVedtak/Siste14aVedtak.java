package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;

import java.time.ZonedDateTime;

@Data
@Builder
@Accessors(chain = true)
@AllArgsConstructor
public class Siste14aVedtak {
    String brukerId;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    ZonedDateTime fattetDato;
    boolean fraArena;

    public static Siste14aVedtak fraKafkaDto(Siste14aVedtakKafkaDto dto) {
        return new Siste14aVedtak(dto.aktorId.get(), dto.innsatsgruppe, dto.hovedmal, dto.fattetDato, dto.fraArena);
    }

    public static Siste14aVedtak fraApiDto(Siste14aVedtakApiDto dto, String brukerId) {
        return new Siste14aVedtak(brukerId, dto.innsatsgruppe, dto.hovedmal, dto.fattetDato, dto.fraArena);
    }
}
