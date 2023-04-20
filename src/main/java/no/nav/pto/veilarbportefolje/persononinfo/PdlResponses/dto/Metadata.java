package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class Metadata {
    private boolean historisk;
    private PdlMaster master;
    private List<Endringer> endringer;
}