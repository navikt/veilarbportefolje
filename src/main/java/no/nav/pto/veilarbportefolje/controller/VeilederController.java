package no.nav.pto.veilarbportefolje.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.util.PortefoljeUtils;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/veileder")
@Tag(name = "Veileder", description = "Portefølje-funksjonalitet på veiledernivå.")
public class VeilederController {
    private final OpensearchService opensearchService;
    private final AuthService authService;
    private final ArbeidslisteService arbeidslisteService;
    private final AktivitetService aktivitetService;

    @PostMapping("/{veilederident}/portefolje")
    @Operation(summary = "Hent portefølje for veileder", description = "Henter en liste med brukere under oppfølging som er tilordnet veilederen.")
    public Portefolje hentPortefoljeForVeileder(
            @PathVariable("veilederident") String veilederIdent,
            @RequestParam("enhet") String enhet,
            @RequestParam(value = "fra", required = false) Integer fra,
            @RequestParam(value = "antall", required = false) Integer antall,
            @RequestParam("sortDirection") String sortDirection,
            @RequestParam("sortField") String sortField,
            @RequestBody Filtervalg filtervalg) {

        ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
        ValideringsRegler.sjekkEnhet(enhet);
        Sorteringsrekkefolge validertSorteringsrekkefolge = ValideringsRegler.sjekkSorteringsrekkefolge(sortDirection);
        Sorteringsfelt validertSorteringsfelt = ValideringsRegler.sjekkSorteringsfelt(sortField);
        authService.innloggetVeilederHarTilgangTilOppfolging();
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(enhet, Optional.of(veilederIdent), validertSorteringsrekkefolge, validertSorteringsfelt, filtervalg, fra, antall);
        List<Bruker> sensurerteBrukereSublist = authService.sensurerBrukere(brukereMedAntall.getBrukere());

        return PortefoljeUtils.buildPortefolje(brukereMedAntall.getAntall(),
                sensurerteBrukereSublist,
                enhet,
                Optional.ofNullable(fra).orElse(0));
    }

    @GetMapping("/{veilederident}/portefolje/statustall")
    @Operation(summary = "Hent statustall for veilederportefølje", description = "Henter statustall på veileder (statistikk for alle brukere under oppfølging tilordnet veilederen).")
    public VeilederPortefoljeStatustallRespons hentVeilederportefoljeStatustall(@PathVariable("veilederident") String veilederIdent, @RequestParam("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return new VeilederPortefoljeStatustallRespons(
                opensearchService.hentStatustallForVeilederPortefolje(veilederIdent, enhet)
        );
    }

    @GetMapping("/{veilederident}/hentArbeidslisteForVeileder")
    @Operation(summary = "Hent arbeidslister for veileder", description = "Henter en liste av arbeidslister for en gitt veileder på en gitt enhet.")
    public List<Arbeidsliste> hentArbeidslisteForVeileder(@PathVariable("veilederident") VeilederId veilederIdent, @RequestParam("enhet") EnhetId enhet) {
        ValideringsRegler.sjekkEnhet(enhet.get());
        ValideringsRegler.sjekkVeilederIdent(veilederIdent.getValue(), false);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet.get());

        return arbeidslisteService.getArbeidslisteForVeilederPaEnhet(enhet, veilederIdent);
    }

    @GetMapping("{veilederident}/moteplan")
    @Operation(summary = "Hent møteplan for veileder", description = "Henter en liste av fremtidige møter for en gitt veileder på en gitt enhet.")
    public List<MoteplanDTO> hentMoteplanForVeileder(@PathVariable("veilederident") VeilederId veilederIdent, @RequestParam("enhet") EnhetId enhet) {
        ValideringsRegler.sjekkEnhet(enhet.get());
        ValideringsRegler.sjekkVeilederIdent(veilederIdent.getValue(), false);

        authService.innloggetVeilederHarTilgangTilEnhet(enhet.get());
        BrukerinnsynTilganger tilgangTilSkjermeteBrukere = authService.hentVeilederBrukerInnsynTilganger();

        return aktivitetService.hentMoteplan(veilederIdent, enhet, tilgangTilSkjermeteBrukere);
    }
}
