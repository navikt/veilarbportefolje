package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Adressebeskyttelse {
    private String gradering;
    private PdlBarnResponse.PdlBarnResponseData.Metadata metadata;
}
