package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PdlDokument {
    PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData hentPerson;
    PdlIdentResponse.HentIdenterResponseData.HentIdenterResponsData hentIdenter;
}
