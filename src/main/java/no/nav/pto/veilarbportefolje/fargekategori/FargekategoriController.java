package no.nav.pto.veilarbportefolje.fargekategori;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FargekategoriController {

    private final FargekategoriService fargekategoriService;
    private final AuthService authService;
    private final BrukerServiceV2 brukerServiceV2;

    @PostMapping("/hent-fargekategori")
    public ResponseEntity<FargekategoriEntity> hentFargekategoriForBruker(@RequestBody HentFargekategoriRequest request) {
        validerRequest(request.fnr);

        Optional<NavKontor> brukerEnhet = brukerServiceV2.hentNavKontor(request.fnr);
        if (brukerEnhet.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bruker med oppgitt fnr er ikke under oppfølging");
        }

        authService.tilgangTilOppfolging();
        authService.tilgangTilBruker(request.fnr.get());
        authService.tilgangTilEnhet(brukerEnhet.get().toString());


        try {
            Optional<FargekategoriEntity> kanskjeFargekategori = fargekategoriService.hentFargekategoriForBruker(request);

            return kanskjeFargekategori.map(ResponseEntity::ok).orElseThrow();
        } catch (Exception e) {
            String melding = String.format("Klarte ikke å hente fargekategori for fnr %s", request.fnr.get());
            secureLog.error(melding, e);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/fargekategori")
    public ResponseEntity<FargekategoriResponse> oppdaterFargekategoriForBruker(@RequestBody OppdaterFargekategoriRequest request) {
        VeilederId innloggetVeileder = AuthUtils.getInnloggetVeilederIdent();
        validerRequest(request.fnr);

        Optional<NavKontor> brukerEnhet = brukerServiceV2.hentNavKontor(request.fnr);

        if (brukerEnhet.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bruker med oppgitt fnr er ikke under oppfølging");
        }

        authService.tilgangTilOppfolging();
        authService.tilgangTilBruker(request.fnr.get());
        authService.tilgangTilEnhet(brukerEnhet.get().toString());
        // TODO berre "tildelt veileder" skal kunne redigere

        try {
            Optional<UUID> fargekategoriId = fargekategoriService.oppdaterFargekategoriForBruker(request, innloggetVeileder);

            return fargekategoriId
                    .map(uuid -> ResponseEntity.ok(new FargekategoriResponse(uuid)))
                    .orElseGet(() -> ResponseEntity.status(204).build());
        } catch (Exception e) {
            String melding = String.format("Klarte ikke å opprette/oppdatere fargekategori med verdi %s for fnr %s", request.fargekategoriVerdi.name(), request.fnr.get());
            secureLog.error(melding, e);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static void validerRequest(Fnr fnr) {
        if (!Fnr.isValid(fnr.get())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldig fnr");
        }
    }

    public record HentFargekategoriRequest(
            @JsonProperty(required = true) Fnr fnr
    ) {
    }

    public record FargekategoriResponse(UUID id) {
    }

    public record OppdaterFargekategoriRequest(
            @JsonProperty(required = true) Fnr fnr,
            @JsonProperty(required = true) FargekategoriVerdi fargekategoriVerdi
    ) {
    }
}
