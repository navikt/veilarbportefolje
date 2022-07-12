package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class Siste14aVedtak {
    String brukerId;
    Siste14aVedtakKafkaDTO.Innsatsgruppe innsatsgruppe;
    Siste14aVedtakKafkaDTO.Hovedmal hovedmal;
    ZonedDateTime fattetDato;
    boolean fraArena;

    public static Siste14aVedtak fraKafkaDto(Siste14aVedtakKafkaDTO dto) {
        return new Siste14aVedtak(dto.aktorId.get(), dto.innsatsgruppe, dto.hovedmal, dto.fattetDato, dto.fraArena);
    }
}
