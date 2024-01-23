package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.RestResponse;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FargekategoriController {

    private final FargekategoriService fargekategoriService;

    @PutMapping("/fargekategori")
    public ResponseEntity<FargekategoriResponse> oppdaterFargekategoriForBruker(@RequestBody OppdaterFargekategoriRequest request) {
        VeilederId innloggetVeileder = AuthUtils.getInnloggetVeilederIdent();
        // TODO: Validering
        // Sjekk at request.fnr er gyldig fnr

        // TODO: Autorisering
        // Sjekk at veileder har tilgang til oppfølging
        // Sjekk at veileder har les/skriv tilgang til bruker
        // Sjekk at veileder har tilgang til enhet (?), muligens dekket av den over
        // Sjekk at veileder er tilordnet brukeren

        return ResponseEntity.ok(new FargekategoriResponse(fargekategoriService.oppdaterFargekategoriForBruker(request, innloggetVeileder)));
    }

    public record FargekategoriResponse(UUID id) {
    }

    public record OppdaterFargekategoriRequest(Fnr fnr, FargekategoriVerdi fargekategoriVerdi) {
    }
}
