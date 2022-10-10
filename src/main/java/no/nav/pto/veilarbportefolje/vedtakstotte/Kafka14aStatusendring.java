package no.nav.pto.veilarbportefolje.vedtakstotte;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kafka14aStatusendring {

    public enum Status {
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


    // Statusendringene er kun for vedtak fra Vedtaksstøtte der det ikke er mulig med hovedmål OKE_DELTAKELSE
    public enum Hovedmal {
        SKAFFE_ARBEID, BEHOLDE_ARBEID
    }

    long vedtakId;
    String aktorId;
    Status vedtakStatusEndring;
    LocalDateTime timestamp;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    String veilederIdent;
    String veilederNavn;

    //DENNE TRENGER VI FOR ATT SORETINGREKKEFOLGEN SKA BLI RIKTIG
    public static String statusTilTekst(Status status) {
        return switch (status) {
            case UTKAST_OPPRETTET, BESLUTTER_PROSESS_AVBRUTT -> "Utkast";
            case BESLUTTER_PROSESS_STARTET -> "Trenger kvalitetssikring";
            case BLI_BESLUTTER, KLAR_TIL_BESLUTTER -> "Venter på tilbakemelding";
            case GODKJENT_AV_BESLUTTER -> "Klar til utsendelse";
            case KLAR_TIL_VEILEDER -> "Venter på veileder";
            default -> null;
        };
    }

}
