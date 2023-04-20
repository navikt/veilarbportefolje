package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Matrikkeladresse {
    private Long matrikkelId;
    private String bruksenhetsnummer;
    private String tilleggsnavn;
    private String postnummer;
    private String kommunenummer;
}
