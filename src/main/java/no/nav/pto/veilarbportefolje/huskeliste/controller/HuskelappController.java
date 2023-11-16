package no.nav.pto.veilarbportefolje.huskeliste.controller;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskeliste.HuskelappService;
import no.nav.pto.veilarbportefolje.huskeliste.HuskelappStatus;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappInputDto;
import no.nav.pto.veilarbportefolje.huskeliste.controller.dto.HuskelappOutputDto;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1")
@RequiredArgsConstructor
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
                UUID uuid = huskelappService.opprettHuskelapp(inputDto);

                return ResponseEntity.status(HttpStatus.CREATED).body(uuid);

            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (RuntimeException) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/hent-huskelapp-paa-enhet")
    public ResponseEntity<List<HuskelappOutputDto>> hentHuskelapp(EnhetId enhetId) {
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

        if (authService.harVeilederTilgangTilEnhet(veilederId.getValue(), enhetId.get())) {
            return huskelappService.hentHuskelapp(enhetId, veilederId);
        }

        throw new RuntimeException("Veileder har ikke tilgang til å se huskelappen til bruker.");
    }

    public ResponseEntity<HuskelappOutputDto> hentHuskelapp(Fnr brukerFnr) {
        boolean erVeilederForBruker = validerErVeilederForBruker(brukerFnr);

        if (erVeilederForBruker) {
            return huskelappService.hentHuskelapp(brukerFnr);
        }

        throw new RuntimeException("Veileder har ikke tilgang til å se huskelappen til bruker.");
    }

    public ResponseEntity slettHuskelapp(String huskelappId) {
        return null;
    }

    public ResponseEntity oppdatereStatus(String huskelappId, HuskelappStatus status) {
        return null;
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
