package no.nav.pto.veilarbportefolje.controller;

import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.util.PortefoljeUtils;
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
@RequestMapping("/api/enhet")
@Produces(APPLICATION_JSON)
public class EnhetController {

    private final ElasticIndexer elasticIndexer;
    private final AuthService authService;
    private final TiltakService tiltakService;
    private final MetricsClient metricsClient;

    @Autowired
    public EnhetController(
            ElasticIndexer indekseringService,
            AuthService authService,
            MetricsClient metricsClient,
            TiltakService tiltakService) {
        this.elasticIndexer = indekseringService;
        this.tiltakService = tiltakService;
        this.authService = authService;
        this.metricsClient = metricsClient;
    }


    @PostMapping("/{enhet}/portefolje")
    public Response hentPortefoljeForEnhet(
            @PathVariable("enhet") String enhet,
            @RequestParam("fra") Integer fra,
            @RequestParam("antall") Integer antall,
            @RequestParam("sortDirection") String sortDirection,
            @RequestParam("sortField") String sortField,
            Filtervalg filtervalg) {

        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            ValideringsRegler.sjekkSortering(sortDirection, sortField);
            ValideringsRegler.sjekkFiltervalg(filtervalg);
            authService.tilgangTilOppfolging();
            authService.tilgangTilEnhet(enhet);

            String ident = authService.getInnloggetVeilederIdent().getVeilederId();
            String identHash = DigestUtils.md5Hex(ident).toUpperCase();

            BrukereMedAntall brukereMedAntall = elasticIndexer.hentBrukere(enhet, Optional.empty(), sortDirection, sortField, filtervalg, fra, antall);
            List<Bruker> sensurerteBrukereSublist = authService.sensurerBrukere(brukereMedAntall.getBrukere());

            Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukereMedAntall.getAntall(),
                    sensurerteBrukereSublist,
                    enhet,
                    Optional.ofNullable(fra).orElse(0));

            Event event = new Event("enhetsportefolje.lastet");
            event.addFieldToReport("identhash", identHash);
            metricsClient.report(event);

            return portefolje;
        });
    }


    @GetMapping("/{enhet}/portefoljestorrelser")
    public Response hentPortefoljestorrelser(@PathVariable("enhet") String enhet) {
        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            authService.tilgangTilEnhet(enhet);

            return elasticIndexer.hentPortefoljestorrelser(enhet);
        });
    }

    @GetMapping("/{enhet}/statustall")
    public Response hentStatusTall(@PathVariable("enhet") String enhet) {
        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            authService.tilgangTilEnhet(enhet);

            return elasticIndexer.hentStatusTallForPortefolje(enhet);
        });
    }

    @GetMapping("/{enhet}/tiltak")
    public Response hentTiltak(@PathVariable("enhet") String enhet) {
        return createResponse(() -> {
            ValideringsRegler.sjekkEnhet(enhet);
            authService.tilgangTilEnhet(enhet);

            return tiltakService.hentEnhettiltak(enhet)
                    .getOrElse(new EnhetTiltak());
        });
    }
}
