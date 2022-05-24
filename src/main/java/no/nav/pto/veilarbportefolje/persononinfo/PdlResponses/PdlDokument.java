package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdlDokument {
    PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData hentPerson;
    PdlIdentResponse.HentIdenterResponseData.HentIdenterResponsData hentIdenter;
}
