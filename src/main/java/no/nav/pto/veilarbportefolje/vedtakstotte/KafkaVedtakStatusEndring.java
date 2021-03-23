package no.nav.pto.veilarbportefolje.vedtakstotte;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaVedtakStatusEndring {

    public enum VedtakStatusEndring {
        UTKAST_OPPRETTET,
        UTKAST_SLETTET,
        VEDTAK_SENDT,
        BESLUTTER_PROSESS_STARTET,
        BESLUTTER_PROSESS_AVBRUTT,
        BLI_BESLUTTER,
        OVERTA_FOR_BESLUTTER, // DENNE SKA VI INTE HANTERA ENN SÅ LENGE
        OVERTA_FOR_VEILEDER,
        GODKJENT_AV_BESLUTTER,
        KLAR_TIL_BESLUTTER,
        KLAR_TIL_VEILEDER
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

    long vedtakId;
    String aktorId;
    VedtakStatusEndring vedtakStatusEndring;
    LocalDateTime timestamp;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    String veilederIdent;
    String veilederNavn;

    //DENNE TRENGER VI FOR ATT SORETINGREKKEFOLGEN SKA BLI RIKTIG
    public static String vedtakStatusTilTekst(VedtakStatusEndring vedtakStatusEndring) {
        switch(vedtakStatusEndring) {
            case UTKAST_OPPRETTET:
            case BESLUTTER_PROSESS_AVBRUTT:
            return "Utkast";
            case BESLUTTER_PROSESS_STARTET:
                return "Trenger kvalitetssikring";
            case BLI_BESLUTTER:
            case KLAR_TIL_BESLUTTER:
                return "Venter på tilbakemelding";
            case GODKJENT_AV_BESLUTTER:
                return "Klar til utsendelse";
            case KLAR_TIL_VEILEDER:
                return "Venter på veileder";
            default:
                return null;
        }
    }

}
