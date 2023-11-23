package no.nav.pto.veilarbportefolje.huskelapp.controller;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.*;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class HuskelappController {

    private final HuskelappService huskelappService;
    private final AuthService authService;
    private final AktorClient aktorClient;
    private final BrukerServiceV2 brukerServiceV2;

    @PostMapping("/huskelapp")
    public ResponseEntity<UUID> opprettHuskelapp(@RequestBody HuskelappOpprettRequest huskelappOpprettRequest) {
        try {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
			validerOppfolgingOgBruker(huskelappOpprettRequest.brukerFnr().get());
            boolean erVeilederForBruker = validerErVeilederForBruker(huskelappOpprettRequest.brukerFnr());

            if (erVeilederForBruker && authService.harVeilederTilgangTilEnhet(veilederId.getValue(), huskelappOpprettRequest.enhetId().get())) {
                UUID uuid = huskelappService.opprettHuskelapp(huskelappOpprettRequest, veilederId);

                return ResponseEntity.status(HttpStatus.CREATED).body(uuid);

            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/huskelapp")
    public ResponseEntity redigerHuskelapp(@RequestBody HuskelappRedigerRequest huskelappRedigerRequest) {
        try {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
            validerOppfolgingOgBruker(huskelappRedigerRequest.brukerFnr().get());

            if (authService.harVeilederTilgangTilEnhet(veilederId.getValue(), huskelappRedigerRequest.enhetId().get())) {
                huskelappService.redigerHuskelapp(huskelappRedigerRequest, veilederId);

                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/hent-huskelapp-for-veileder")
    public ResponseEntity<List<HuskelappOutputDto>> hentHuskelapp(@RequestBody HuskelappForVeilederRequest huskelappForVeilederRequest) {
        try {
            VeilederId veilederId = huskelappForVeilederRequest.veilederId();

            if (authService.harVeilederTilgangTilEnhet(veilederId.getValue(), huskelappForVeilederRequest.enhetId().get())) {
                List<HuskelappOutputDto> huskelappOutputDtoList = huskelappService.hentHuskelapp(veilederId, huskelappForVeilederRequest.enhetId());
                return ResponseEntity.ok(huskelappOutputDtoList);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder har ikke tilgang til å se huskelappen til bruker.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/hent-huskelapp-for-bruker")
    public ResponseEntity<HuskelappOutputDto> hentHuskelapp(@RequestBody HuskelappForBrukerRequest huskelappForBrukerRequest) {
        try {
            validerOppfolgingOgBruker(huskelappForBrukerRequest.fnr().get());
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
            Fnr brukerFnr = huskelappForBrukerRequest.fnr();

            boolean harVeilederTilgang = brukerServiceV2.hentNavKontor(brukerFnr)
                    .map(enhet -> authService.harVeilederTilgangTilEnhet(veilederId.getValue(), enhet.getValue()))
                    .orElse(false);

            if (harVeilederTilgang) {
                HuskelappOutputDto huskelappOutputDto = huskelappService.hentHuskelapp(brukerFnr);
                return ResponseEntity.ok(huskelappOutputDto);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder har ikke tilgang til å se huskelappen til bruker.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/huskelapp")
    public ResponseEntity<String> slettHuskelapp(HuskelappSlettRequest huskelappSlettRequest) {
        try {
            Optional<HuskelappOutputDto> huskelappOptional = huskelappService.hentHuskelapp(UUID.fromString(huskelappSlettRequest.huskelappId()));

            if (huskelappOptional.isPresent()) {
                VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
				validerOppfolgingOgBruker(huskelappOptional.get().brukerFnr().get());
                boolean erVeilederForBruker = validerErVeilederForBruker(huskelappOptional.get().brukerFnr());
                boolean harTilgangTilEnhet = authService.harVeilederTilgangTilEnhet(veilederId.getValue(), huskelappOptional.get().enhetID().get());

                if (erVeilederForBruker && harTilgangTilEnhet) {
                    huskelappService.settHuskelappIkkeAktiv(UUID.fromString(huskelappSlettRequest.huskelappId()), huskelappOptional.get().brukerFnr());
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                }
            }

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder har ikke tilgang til å slette huskelappen til bruker.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean validerErVeilederForBruker(Fnr fnr) {
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

        return ValideringsRegler
                .validerFnr(fnr.get())
                .map(this::hentAktorId)
                .get()
                .map(brukerServiceV2::hentVeilederForBruker)
                .get()
                .map(currentVeileder -> currentVeileder.equals(veilederId))
                .orElse(false);

    }

    private Try<AktorId> hentAktorId(Fnr fnr) {
        return Try.of(() -> aktorClient.hentAktorId(fnr));
    }

    private void validerOppfolgingOgBruker(String fnr) {
        authService.tilgangTilOppfolging();
        Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
        authService.tilgangTilBruker(fnr);
        if (validateFnr.isInvalid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
