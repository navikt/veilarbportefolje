package no.nav.pto.veilarbportefolje.controller;

import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.util.PortefoljeUtils;
import no.nav.common.metrics.Event;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.pto.veilarbportefolje.util.RestUtils.createResponse;

@RestController
@RequestMapping("/api/veileder")
@Produces(APPLICATION_JSON)
public class VeilederController {

    private ElasticIndexer elasticIndexer;
    private AuthService authService;
    private MetricsClient metricsClient;

    @Autowired
    public VeilederController(
            ElasticIndexer elasticIndexer,
            AuthService authService,
            MetricsClient metricsClient
    ) {

        this.elasticIndexer = elasticIndexer;
        this.authService = authService;
        this.metricsClient = metricsClient;
    }

    @PostMapping("/{veilederident}/portefolje")
    public Portefolje hentPortefoljeForVeileder(
            @PathVariable("veilederident") String veilederIdent,
            @RequestParam("enhet") String enhet,
            @RequestParam("fra") Integer fra,
            @RequestParam("antall") Integer antall,
            @RequestParam("sortDirection") String sortDirection,
            @RequestParam("sortField") String sortField,
            @RequestBody Filtervalg filtervalg) {


        ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
        ValideringsRegler.sjekkEnhet(enhet);
        ValideringsRegler.sjekkSortering(sortDirection, sortField);
        ValideringsRegler.sjekkFiltervalg(filtervalg);
        authService.tilgangTilOppfolging();
        authService.tilgangTilEnhet(enhet);

        String ident = authService.getInnloggetVeilederIdent().getVeilederId();
        String identHash = DigestUtils.md5Hex(ident).toUpperCase();

        BrukereMedAntall brukereMedAntall = elasticIndexer.hentBrukere(enhet, Optional.of(veilederIdent), sortDirection, sortField, filtervalg, fra, antall);
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

        return elasticIndexer.hentStatusTallForVeileder(enhet, veilederIdent);
    }

    @GetMapping("/{veilederident}/arbeidsliste")
    public List<Bruker>  hentArbeidsliste(@PathVariable("veilederident") String veilederIdent, @RequestParam("enhet") String enhet) {
        Event event = new Event("minoversiktportefolje.arbeidsliste.lastet");
        metricsClient.report(event);
        ValideringsRegler.sjekkEnhet(enhet);
        ValideringsRegler.sjekkVeilederIdent(veilederIdent, false);
        authService.tilgangTilEnhet(enhet);

        return elasticIndexer.hentBrukereMedArbeidsliste(VeilederId.of(veilederIdent), enhet);
    }

}
