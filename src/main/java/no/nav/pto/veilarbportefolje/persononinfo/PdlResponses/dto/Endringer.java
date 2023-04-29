package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Endringer {
    private String registrert;
}
