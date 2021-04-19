package no.nav.pto.veilarbportefolje.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.postgres.PostgresService;
import no.nav.pto.veilarbportefolje.util.PortefoljeUtils;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class EnhetControllerV2 {
    private final AuthService authService;
    private final TiltakService tiltakService;
    private final MetricsClient metricsClient;
    private final PostgresService postgresService;

    @PostMapping("v2/{enhet}/portefolje")
    public Portefolje hentPortefoljeForEnhet(
            @PathVariable("enhet") String enhet,
            @RequestParam(value = "fra", required = false) Integer fra,
            @RequestParam(value = "antall", required = false) Integer antall,
            @RequestParam("sortDirection") String sortDirection,
            @RequestParam("sortField") String sortField,
            @RequestBody Filtervalg filtervalg) {

        ValideringsRegler.sjekkEnhet(enhet);
        ValideringsRegler.sjekkSortering(sortDirection, sortField);
        ValideringsRegler.sjekkFiltervalg(filtervalg);
        authService.tilgangTilOppfolging();
        authService.tilgangTilEnhet(enhet);

        String ident = AuthUtils.getInnloggetVeilederIdent().toString();
        String identHash = DigestUtils.md5Hex(ident).toUpperCase();

        BrukereMedAntall brukereMedAntall = postgresService.hentBrukere(enhet, Optional.empty(), sortDirection, sortField, filtervalg, fra, antall);
        List<Bruker> sensurerteBrukereSublist = authService.sensurerBrukere(brukereMedAntall.getBrukere());

        Portefolje portefolje = PortefoljeUtils.buildPortefolje(brukereMedAntall.getAntall(),
                sensurerteBrukereSublist,
                enhet,
                Optional.ofNullable(fra).orElse(0));

        Event event = new Event("enhetsportefolje.lastet");
        event.addFieldToReport("identhash", identHash);
        metricsClient.report(event);

        return portefolje;
    }


    @GetMapping("v2/{enhet}/portefoljestorrelser")
    public FacetResults hentPortefoljestorrelser(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.tilgangTilEnhet(enhet);

        return postgresService.hentPortefoljestorrelser(enhet);
    }

    @GetMapping("v2/{enhet}/statustall")
    public StatusTall hentStatusTall(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.tilgangTilEnhet(enhet);

        return postgresService.hentStatusTallForEnhet(enhet);

    }

    @GetMapping("v2/{enhet}/tiltak")
    public EnhetTiltak hentTiltak(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.tilgangTilEnhet(enhet);

        return tiltakService.hentEnhettiltak(enhet)
                .getOrElse(new EnhetTiltak());
    }
}
