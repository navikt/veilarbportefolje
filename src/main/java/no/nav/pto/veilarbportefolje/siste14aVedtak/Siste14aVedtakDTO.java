package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class Siste14aVedtakDTO {
    AktorId aktorId;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    ZonedDateTime fattetDato;
    boolean fraArena;

    public enum Innsatsgruppe {
        STANDARD_INNSATS,
        SITUASJONSBESTEMT_INNSATS,
        SPESIELT_TILPASSET_INNSATS,
        GRADERT_VARIG_TILPASSET_INNSATS,
        VARIG_TILPASSET_INNSATS
    }

    public enum Hovedmal {
        SKAFFE_ARBEID, BEHOLDE_ARBEID, OKE_DELTAKELSE;
    }
}
