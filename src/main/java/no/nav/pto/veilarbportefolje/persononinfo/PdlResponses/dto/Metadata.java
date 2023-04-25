package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {
    private boolean historisk;
    private PdlMaster master;
    private List<PdlBarnResponse.PdlBarnResponseData.Endringer> endringer;
}