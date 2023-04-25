package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vegadresse {
    private Long matrikkelId;
    private String husbokstav;
    private String husnummer;
    private String adressenavn;
    private String bruksenhetsnummer;
    private String tilleggsnavn;
    private String postnummer;
    private String kommunenummer;
    private String bydelsnummer;
}
