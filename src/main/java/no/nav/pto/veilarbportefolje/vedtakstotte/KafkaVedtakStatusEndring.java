package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.Hovedmal;
import no.nav.pto.veilarbportefolje.domene.Innsatsgruppe;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class KafkaVedtakStatusEndring {

    public enum KafkaVedtakStatus {
        UTKAST_OPPRETTET, SENDT_TIL_BESLUTTER, SENDT_TIL_BRUKER, UTKAST_SLETTET
    }
    long vedtakId;
    String aktorId;
    KafkaVedtakStatus vedtakStatus;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    LocalDateTime statusEndretTidspunkt;
}
