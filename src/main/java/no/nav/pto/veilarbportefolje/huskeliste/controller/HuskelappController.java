package no.nav.pto.veilarbportefolje.huskeliste.controller;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskeliste.HuskelappService;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappInputDto;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappOutputDto;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappUpdateStatusDto;
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
    public ResponseEntity<UUID> opprettHuskelapp(@RequestBody HuskelappInputDto inputDto) {
        try {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();
            boolean erVeilederForBruker = validerErVeilederForBruker(inputDto.brukerFnr());

            if (erVeilederForBruker && authService.harVeilederTilgangTilEnhet(veilederId.getValue(), inputDto.enhetId().get())) {
                UUID uuid = huskelappService.opprettHuskelapp(inputDto, veilederId);

                return ResponseEntity.status(HttpStatus.CREATED).body(uuid);

            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/hent-huskelapp-paa-enhet")
    public ResponseEntity<List<HuskelappOutputDto>> hentHuskelapp(EnhetId enhetId) {
        try {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

            if (authService.harVeilederTilgangTilEnhet(veilederId.getValue(), enhetId.get())) {
                List<HuskelappOutputDto> huskelappOutputDtoList = huskelappService.hentHuskelapp(veilederId, enhetId);
                return ResponseEntity.ok(huskelappOutputDtoList);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder har ikke tilgang til å se huskelappen til bruker.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/hent-huskelapp-paa-bruker")
    public ResponseEntity<HuskelappOutputDto> hentHuskelapp(Fnr brukerFnr) {
        try {
            VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

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
    public ResponseEntity<String> slettHuskelapp(String huskelappId) {
        try {
            Optional<HuskelappOutputDto> huskelappOptional = huskelappService.hentHuskelapp(UUID.fromString(huskelappId));

            if (huskelappOptional.isPresent()) {
                boolean erVeilederForBruker = validerErVeilederForBruker(huskelappOptional.get().brukerFnr());

                if (erVeilederForBruker) {
                    huskelappService.slettHuskelapp(UUID.fromString(huskelappId));
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                }
            }

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder har ikke tilgang til å slette huskelappen til bruker.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/huskelapp")
    public ResponseEntity<String> oppdatereStatus(@RequestBody HuskelappUpdateStatusDto huskelappUpdateStatusDto) {

        try {
            Optional<HuskelappOutputDto> huskelappOptional = huskelappService.hentHuskelapp(UUID.fromString(huskelappUpdateStatusDto.huskelappId()));

            if (huskelappOptional.isPresent()) {
                boolean erVeilederForBruker = validerErVeilederForBruker(huskelappOptional.get().brukerFnr());

                if (erVeilederForBruker) {
                    huskelappService.oppdatereStatus(UUID.fromString(huskelappUpdateStatusDto.huskelappId()), huskelappOptional.get().brukerFnr(), huskelappUpdateStatusDto.status());
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                }
            }

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder har ikke tilgang til å oppdatere huskelappen til bruker.");
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
}
