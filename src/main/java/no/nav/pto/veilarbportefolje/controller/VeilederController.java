package no.nav.pto.veilarbportefolje.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.auth.Skjermettilgang;
import no.nav.pto.veilarbportefolje.domene.Bruker;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.Portefolje;
import no.nav.pto.veilarbportefolje.domene.StatusTall;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.util.PortefoljeUtils;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static no.nav.common.json.JsonUtils.toJson;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/veileder")
public class VeilederController {
    private final OpensearchService opensearchService;
    private final AuthService authService;
    private final MetricsClient metricsClient;
    private final ArbeidslisteService arbeidslisteService;
    private final AktivitetService aktivitetService;

    @PostMapping("/{veilederident}/portefolje")
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
        ValideringsRegler.sjekkSortering(sortDirection, sortField);
        ValideringsRegler.sjekkFiltervalg(filtervalg);
        authService.tilgangTilOppfolging();
        authService.tilgangTilEnhet(enhet);

        String ident = AuthUtils.getInnloggetVeilederIdent().toString();
        String identHash = DigestUtils.md5Hex(ident).toUpperCase();

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(enhet, Optional.of(veilederIdent), sortDirection, sortField, filtervalg, fra, antall);
        List<Bruker> sensurerteBrukereSublist = authService.sensurerBrukere(brukereMedAntall.getBrukere());

        Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukereMedAntall.getAntall(),
                sensurerteBrukereSublist,
                enhet,
                Optional.ofNullable(fra).orElse(0));

        Event event = new Event("minoversiktportefolje.lastet");
        event.addFieldToReport("identhash", identHash);
        metricsClient.report(event);

        return portefolje;
    }

    @GetMapping("/{veilederident}/statustall")
    public StatusTall hentStatusTall(@PathVariable("veilederident") String veilederIdent, @RequestParam("enhet") String enhet) {
        Event event = new Event("minoversiktportefolje.statustall.lastet");
        metricsClient.report(event);
        ValideringsRegler.sjekkEnhet(enhet);
        ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
        authService.tilgangTilEnhet(enhet);

        return opensearchService.hentStatusTallForVeileder(veilederIdent, enhet);
    }

    @GetMapping("/{veilederident}/hentArbeidslisteForVeileder")
    public List<Arbeidsliste> hentArbeidslisteForVeileder(@PathVariable("veilederident") VeilederId veilederIdent, @RequestParam("enhet") EnhetId enhet) {
        ValideringsRegler.sjekkEnhet(enhet.get());
        ValideringsRegler.sjekkVeilederIdent(veilederIdent.getValue(), false);
        authService.tilgangTilEnhet(enhet.get());

        return arbeidslisteService.getArbeidslisteForVeilederPaEnhet(enhet, veilederIdent);
    }

    @GetMapping("{veilederident}/moteplan")
    public String hentMoteplanForVeileder(@PathVariable("veilederident") VeilederId veilederIdent, @RequestParam("enhet") EnhetId enhet){
        ValideringsRegler.sjekkEnhet(enhet.get());
        ValideringsRegler.sjekkVeilederIdent(veilederIdent.getValue(), false);

        authService.tilgangTilEnhet(enhet.get());
        Skjermettilgang tilgangTilSkjermeteBrukere = authService.hentVeilederTilgangTilSkjermet();

        return toJson(aktivitetService.hentMoteplan(veilederIdent, enhet, tilgangTilSkjermeteBrukere));
    }
}
