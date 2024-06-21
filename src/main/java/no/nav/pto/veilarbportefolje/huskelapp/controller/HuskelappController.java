package no.nav.pto.veilarbportefolje.huskelapp.controller;

import io.vavr.control.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappService;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.*;
import no.nav.pto.veilarbportefolje.huskelapp.domain.Huskelapp;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class HuskelappController {

    private final HuskelappService huskelappService;
    private final AuthService authService;
    private final BrukerServiceV2 brukerServiceV2;

    @PostMapping("/huskelapp")
    public ResponseEntity<HuskelappOpprettResponse> opprettHuskelapp(@RequestBody HuskelappOpprettRequest huskelappOpprettRequest) {
        validerOppfolgingOgBrukerOgEnhet(huskelappOpprettRequest.brukerFnr().get(), huskelappOpprettRequest.enhetId().get());
        try {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

            if (!harBrukerenTildeltVeileder(huskelappOpprettRequest.brukerFnr())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if ((huskelappOpprettRequest.kommentar() == null || huskelappOpprettRequest.kommentar().isEmpty()) && huskelappOpprettRequest.frist() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            UUID uuid = huskelappService.opprettHuskelapp(huskelappOpprettRequest, veilederId);
            return ResponseEntity.status(HttpStatus.CREATED).body(new HuskelappOpprettResponse(uuid.toString()));

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/huskelapp")
    public ResponseEntity redigerHuskelapp(@RequestBody HuskelappRedigerRequest huskelappRedigerRequest) {
        validerOppfolgingOgBrukerOgEnhet(huskelappRedigerRequest.brukerFnr().get(), huskelappRedigerRequest.enhetId().get());
        try {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

            if (!harBrukerenTildeltVeileder(huskelappRedigerRequest.brukerFnr())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (huskelappRedigerRequest.huskelappId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Huskelapp kan ikke redigeres nå.");
            }
            if ((huskelappRedigerRequest.kommentar() == null || huskelappRedigerRequest.kommentar().isEmpty()) && huskelappRedigerRequest.frist() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Huskelapp mangler frist og kommentar.");
            }

            huskelappService.redigerHuskelapp(huskelappRedigerRequest, veilederId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/hent-huskelapp-for-veileder")
    public ResponseEntity<List<HuskelappResponse>> hentHuskelapp(@RequestBody HuskelappForVeilederRequest huskelappForVeilederRequest) {
        authService.innloggetVeilederHarTilgangTilEnhet(huskelappForVeilederRequest.enhetId().get());
        try {
            List<HuskelappResponse> huskelappList = huskelappService.hentHuskelapp(huskelappForVeilederRequest.veilederId(), huskelappForVeilederRequest.enhetId()).stream().map(this::mapToHuskelappResponse).collect(Collectors.toList());
            return ResponseEntity.ok(huskelappList);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/hent-huskelapp-for-bruker")
    public ResponseEntity<HuskelappResponse> hentHuskelapp(@RequestBody HuskelappForBrukerRequest huskelappForBrukerRequest) {
        validerOppfolgingOgBrukerOgEnhet(huskelappForBrukerRequest.fnr().get(), huskelappForBrukerRequest.enhetId().get());
        try {
            if (!harBrukerenTildeltVeileder(huskelappForBrukerRequest.fnr())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Optional<Huskelapp> huskelapp = huskelappService.hentHuskelapp(huskelappForBrukerRequest.fnr());
            return huskelapp.map(value -> ResponseEntity.ok(mapToHuskelappResponse(value))).orElseGet(() -> ResponseEntity.ok(null));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/huskelapp")
    public ResponseEntity<String> slettHuskelapp(@RequestBody HuskelappSlettRequest huskelappSlettRequest) {
        Optional<Huskelapp> huskelappOptional = huskelappService.hentHuskelapp(UUID.fromString(huskelappSlettRequest.huskelappId()));

        if (huskelappOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        if (!harBrukerenTildeltVeileder(huskelappOptional.get().brukerFnr())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        validerOppfolgingOgBrukerOgEnhet(huskelappOptional.get().brukerFnr().get(), huskelappOptional.get().enhetId().get());

        huskelappService.settHuskelappIkkeAktiv(UUID.fromString(huskelappSlettRequest.huskelappId()), huskelappOptional.get().brukerFnr());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    public Boolean harBrukerenTildeltVeileder(Fnr fnr) {
        Optional<AktorId> aktorId = brukerServiceV2.hentAktorId(fnr);
        if (aktorId.isPresent()) {
            Optional<VeilederId> veilederId = brukerServiceV2.hentVeilederForBruker(aktorId.get());
            return veilederId.isPresent();
        }
        return false;
    }

    private void validerOppfolgingOgBrukerOgEnhet(String fnr, String enhetId) {
        authService.innloggetVeilederHarTilgangTilOppfolging();
        Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
        authService.innloggetVeilederHarTilgangTilBruker(fnr);
        authService.innloggetVeilederHarTilgangTilEnhet(enhetId);
        if (validateFnr.isInvalid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private HuskelappResponse mapToHuskelappResponse(Huskelapp huskelapp) {
        return new HuskelappResponse(
                huskelapp.huskelappId().toString(),
                huskelapp.brukerFnr(),
                huskelapp.enhetId(),
                huskelapp.frist(),
                huskelapp.kommentar(),
                huskelapp.endretDato(),
                huskelapp.endretAv().getValue()
        );
    }
}
