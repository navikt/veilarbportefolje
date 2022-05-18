package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class PdlDokument {
    Set<String> tags;
    PdlPersonResponse.PdlPersonResponseData.HentPersonResponsData hentPerson;
    PdlIdentResponse.HentIdenterResponseData.HentIdenterResponsData hentIdenter;
}
