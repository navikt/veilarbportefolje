package no.nav.pto.veilarbportefolje.fargekategori;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        NavKontor navKontorForBruker = brukerServiceV2.hentNavKontor(request.fnr).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bruker er ikke tilordnet enhet"));

        authService.innloggetVeilederHarTilgangTilOppfolging();
        authService.innloggetVeilederHarTilgangTilBruker(request.fnr.get());
        authService.innloggetVeilederHarTilgangTilEnhet(navKontorForBruker.getValue());
        try {
            if (!harBrukerenTildeltVeileder(request.fnr())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Optional<FargekategoriEntity> kanskjeFargekategori = fargekategoriService.hentFargekategoriForBruker(request);

            return kanskjeFargekategori.map(ResponseEntity::ok).orElse(ResponseEntity.ok(null));
        } catch (Exception e) {
            String melding = String.format("Klarte ikke å hente fargekategori for fnr %s", request.fnr.get());
            secureLog.error(melding, e);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/fargekategorier")
    public ResponseEntity<BatchUpsertResponse> batchoppdaterFargekategoriForBruker(@RequestBody BatchoppdaterFargekategoriRequest request) {
        VeilederId innloggetVeileder = AuthUtils.getInnloggetVeilederIdent();
        authService.innloggetVeilederHarTilgangTilOppfolging();

        BatchUpsertResponse responseEtterValidering = validerRequest(request);
        if (responseEtterValidering.data.isEmpty()) {
            return ResponseEntity.status(400).body(responseEtterValidering);
        }

        BatchUpsertResponse responseEtterAutoriseringssjekk = sjekkVeilederautorisering(responseEtterValidering.data, request.fargekategoriVerdi);
        List<Fnr> feilFraValideringOgAutorisering = Stream.concat(responseEtterValidering.errors.stream(), responseEtterAutoriseringssjekk.errors.stream()).toList();
        BatchUpsertResponse resultatFraValideringOgAutorisering = new BatchUpsertResponse(responseEtterAutoriseringssjekk.data, feilFraValideringOgAutorisering, request.fargekategoriVerdi);

        if (responseEtterAutoriseringssjekk.data.isEmpty()) {
            return ResponseEntity.status(403).body(resultatFraValideringOgAutorisering);
        }

        Set<NavKontor> brukerEnheter = responseEtterAutoriseringssjekk.data.stream()
                .map(brukerServiceV2::hentNavKontor)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (brukerEnheter.size() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kan ikke oppdatere fargekategori i flere enheter samtidig.");
        }

        EnhetId enhetId = EnhetId.of(brukerEnheter.iterator().next().getValue());
        authService.innloggetVeilederHarTilgangTilEnhet(enhetId.get());

        try {
            fargekategoriService.batchoppdaterFargekategoriForBruker(request.fargekategoriVerdi, responseEtterAutoriseringssjekk.data, innloggetVeileder, enhetId);

            return ResponseEntity.ok(resultatFraValideringOgAutorisering);

        } catch (Exception e) {
            String melding = String.format("Klarte ikke å opprette/oppdatere fargekategori med verdi %s for fnr %s",
                    request.fargekategoriVerdi.name(),
                    request.fnr);
            secureLog.error(melding, e);

            return ResponseEntity.internalServerError().body(new BatchUpsertResponse(Collections.emptyList(), request.fnr, request.fargekategoriVerdi));
        }
    }

    private BatchUpsertResponse validerRequest(BatchoppdaterFargekategoriRequest request) {
        Set<Fnr> sjekkGikkOK = new java.util.HashSet<>(Collections.emptySet());
        Set<Fnr> sjekkFeilet = new java.util.HashSet<>(Collections.emptySet());

        request.fnr.forEach(fnr -> {
            try {
                validerRequest(fnr);

                sjekkGikkOK.add(fnr);
            } catch (Exception e) {
                sjekkFeilet.add(fnr);
            }
        });

        return new BatchUpsertResponse(sjekkGikkOK.stream().toList(), sjekkFeilet.stream().toList(), request.fargekategoriVerdi);
    }

    private BatchUpsertResponse sjekkVeilederautorisering(List<Fnr> fodselsnumre, FargekategoriVerdi fargekategoriVerdi) {
        Set<Fnr> sjekkGikkOK = new java.util.HashSet<>(Collections.emptySet());
        Set<Fnr> sjekkFeilet = new java.util.HashSet<>(Collections.emptySet());

        fodselsnumre.forEach(fnr -> {
            try {

                /* Vi sjekkar om bruker er under oppfølging i autorisering i staden for i validering
                 * for å unngå at feilmeldinga avslører om eit fnr er i systemet. (400 bad request vs 403 forbidden) */

                authService.innloggetVeilederHarTilgangTilBruker(fnr.get());
                NavKontor navKontor = brukerServiceV2.hentNavKontor(fnr).orElseThrow(() -> new IllegalStateException("Brukeren har ikke tildelt enhet"));

                authService.innloggetVeilederHarTilgangTilEnhet(navKontor.getValue());
                sjekkGikkOK.add(fnr);
            } catch (Exception e) {
                sjekkFeilet.add(fnr);
            }
        });

        return new BatchUpsertResponse(sjekkGikkOK.stream().toList(), sjekkFeilet.stream().toList(), fargekategoriVerdi);
    }

    public Boolean harBrukerenTildeltVeileder(Fnr fnr) {
        Optional<AktorId> aktorId = brukerServiceV2.hentAktorId(fnr);
        if (aktorId.isPresent()) {
            Optional<VeilederId> veilederId = brukerServiceV2.hentVeilederForBruker(aktorId.get());
            return veilederId.isPresent();
        }
        return false;
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

    public record FargekategoriResponse(Fnr fnr, FargekategoriVerdi fargekategoriVerdi) {
    }

    public record OppdaterFargekategoriRequest(
            @JsonProperty(required = true) Fnr fnr,
            @JsonProperty(required = true) FargekategoriVerdi fargekategoriVerdi
    ) {
    }

    public record BatchoppdaterFargekategoriRequest(
            @JsonProperty(required = true) List<Fnr> fnr,
            @JsonProperty(required = true) FargekategoriVerdi fargekategoriVerdi
    ) {
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public BatchoppdaterFargekategoriRequest {
        }
    }

    public record BatchUpsertResponse(List<Fnr> data, List<Fnr> errors, FargekategoriVerdi fargekategoriVerdi) {
    }
}
