package no.nav.pto.veilarbportefolje.huskelapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
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

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Huskelapp", description = "Huskelapp-funksjonalitet")
public class HuskelappController {

    private final HuskelappService huskelappService;
    private final AuthService authService;
    private final BrukerServiceV2 brukerServiceV2;
    private final VeilarbVeilederClient veilarbVeilederClient;

    @PostMapping("/huskelapp")
    @Operation(summary = "Opprett huskelapp", description = "Oppretter en huskelapp for en gitt bruker på en gitt enhet.")
    public ResponseEntity<HuskelappOpprettResponse> opprettHuskelapp(@RequestBody HuskelappOpprettRequest huskelappOpprettRequest) {
        validerOppfolgingOgBrukerOgEnhet(huskelappOpprettRequest.brukerFnr().get());
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
    @Operation(summary = "Oppdater huskelapp", description = "Oppdaterer huskelappen for en gitt bruker med nye felter.")
    public ResponseEntity redigerHuskelapp(@RequestBody HuskelappRedigerRequest huskelappRedigerRequest) {
        validerOppfolgingOgBrukerOgEnhet(huskelappRedigerRequest.brukerFnr().get());
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

    @PostMapping("/hent-huskelapp-for-bruker")
    @Operation(summary = "Hent huskelapp for bruker", description = "Henter aktiv huskelapp for en gitt bruker.")
    public ResponseEntity<HuskelappResponse> hentHuskelapp(@RequestBody HuskelappForBrukerRequest huskelappForBrukerRequest) {
        validerOppfolgingOgBrukerOgEnhet(huskelappForBrukerRequest.fnr().get());
        try {
            Optional<Huskelapp> huskelapp = huskelappService.hentHuskelapp(huskelappForBrukerRequest.fnr());
            return huskelapp.map(value -> ResponseEntity.ok(mapToHuskelappResponse(value))).orElseGet(() -> ResponseEntity.ok(null));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/huskelapp")
    @Operation(summary = "Slett huskelapp", description = "Setter huskelapp som inaktiv i databasen og sletter den fra søkemotoren (OpenSearch).")
    public ResponseEntity<String> slettHuskelapp(@RequestBody HuskelappSlettRequest huskelappSlettRequest) {
        Optional<Huskelapp> huskelappOptional = huskelappService.hentHuskelapp(UUID.fromString(huskelappSlettRequest.huskelappId()));

        if (huskelappOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        if (!harBrukerenTildeltVeileder(huskelappOptional.get().brukerFnr())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        validerOppfolgingOgBrukerOgEnhet(huskelappOptional.get().brukerFnr().get());

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

    @PostMapping("/hent-er-bruker-ufordelt")
    @Operation(summary = "Hent om bruker er ufordelt", description = "Sjekker om bruker er ufordelt og returnerer true hvis bruker er ufordelt.")
    public ResponseEntity<Boolean> hentErBrukerUfordelt(@RequestBody HentErBrukerUfordelt request) {
        Optional<NavKontor> navKontor = brukerServiceV2.hentNavKontor(request.fnr);
        if (navKontor.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        List<String> veiledereMedTilgangTilEnhet = veilarbVeilederClient.hentVeilederePaaEnhet(EnhetId.of(navKontor.get().getValue()));
        Optional<AktorId> aktorId = brukerServiceV2.hentAktorId(request.fnr);
        if (aktorId.isEmpty()) {
            secureLog.info("Kunne ikke mappe fnr til aktorId: {}", request.fnr);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Optional<VeilederId> veilederId = brukerServiceV2.hentVeilederForBruker(aktorId.get());
        boolean harVeilederPaaSammeEnhet = veilederId.isPresent() && veiledereMedTilgangTilEnhet.contains(veilederId.get().getValue());
        return ResponseEntity.ok(!harVeilederPaaSammeEnhet);
    }

    public record HentErBrukerUfordelt(Fnr fnr) {
    }

    private void validerOppfolgingOgBrukerOgEnhet(String fnr) {
        Validation<String, Fnr> validateFnr = ValideringsRegler.validerFnr(fnr);
        if (validateFnr.isInvalid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        authService.innloggetVeilederHarTilgangTilOppfolging();
        authService.innloggetVeilederHarTilgangTilBruker(fnr);

        NavKontor navKontor = brukerServiceV2.hentNavKontor(Fnr.of(fnr)).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        authService.innloggetVeilederHarTilgangTilEnhet(navKontor.getValue());
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
