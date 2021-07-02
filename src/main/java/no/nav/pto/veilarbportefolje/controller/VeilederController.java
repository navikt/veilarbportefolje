package no.nav.pto.veilarbportefolje.controller;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.util.PortefoljeUtils;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/veileder")
public class VeilederController {

    private ElasticService elasticService;
    private AuthService authService;
    private MetricsClient metricsClient;

    @Autowired
    public VeilederController(
            ElasticService elasticIndexer,
            AuthService authService,
            MetricsClient metricsClient
    ) {

        this.elasticService = elasticIndexer;
        this.authService = authService;
        this.metricsClient = metricsClient;
    }

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

        BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(enhet, Optional.of(veilederIdent), sortDirection, sortField, filtervalg, fra, antall);
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

        return elasticService.hentStatusTallForVeileder(veilederIdent, enhet);
    }

    @GetMapping("/{veilederident}/arbeidsliste")
    public List<Bruker> hentArbeidsliste(@PathVariable("veilederident") String veilederIdent, @RequestParam("enhet") String enhet) {
        Event event = new Event("minoversiktportefolje.arbeidsliste.lastet");
        metricsClient.report(event);
        ValideringsRegler.sjekkEnhet(enhet);
        ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
        authService.tilgangTilEnhet(enhet);

        return elasticService.hentBrukereMedArbeidsliste(veilederIdent, enhet);
    }

}
