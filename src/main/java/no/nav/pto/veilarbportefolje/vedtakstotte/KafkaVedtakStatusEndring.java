package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class KafkaVedtakStatusEndring {

    public enum KafkaVedtakStatus {
        UTKAST_OPPRETTET, SENDT_TIL_BESLUTTER, SENDT_TIL_BRUKER, UTKAST_SLETTET
    }

    public enum Hovedmal {
        SKAFFE_ARBEID, BEHOLDE_ARBEID
    }

    public enum Innsatsgruppe {
        STANDARD_INNSATS,
        SITUASJONSBESTEMT_INNSATS,
        SPESIELT_TILPASSET_INNSATS,
        GRADERT_VARIG_TILPASSET_INNSATS,
        VARIG_TILPASSET_INNSATS
    }

    public
    long vedtakId;
    String aktorId;
    KafkaVedtakStatus vedtakStatus;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    LocalDateTime statusEndretTidspunkt;

}
