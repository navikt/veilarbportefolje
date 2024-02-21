package no.nav.pto.veilarbportefolje.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.persononinfo.bosted.BostedService;
import no.nav.pto.veilarbportefolje.persononinfo.personopprinelse.PersonOpprinnelseService;
import no.nav.pto.veilarbportefolje.util.PortefoljeUtils;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ;
import static no.nav.pto.veilarbportefolje.opensearch.BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enhet")
public class EnhetController {
    private final OpensearchService opensearchService;
    private final AuthService authService;
    private final TiltakService tiltakService;
    private final PersonOpprinnelseService personOpprinnelseService;
    private final BostedService bostedService;

    private final Cache<String, List<Foedeland>> enhetFoedelandCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private final Cache<String, List<TolkSpraak>> enhetTolkSpraakCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private final Cache<String, List<GeografiskBosted>> enhetGeografiskBostedCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @PostMapping("/{enhet}/portefolje")
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
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(enhet, Optional.empty(), sortDirection, sortField, filtervalg, fra, antall);
        List<Bruker> sensurerteBrukereSublist = authService.sensurerBrukere(brukereMedAntall.getBrukere());

        return PortefoljeUtils.buildPortefolje(brukereMedAntall.getAntall(),
                sensurerteBrukereSublist,
                enhet,
                Optional.ofNullable(fra).orElse(0));
    }

    @GetMapping("/{enhet}/portefoljestorrelser")
    public FacetResults hentPortefoljestorrelser(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return opensearchService.hentPortefoljestorrelser(enhet);
    }

    @GetMapping("/{enhet}/portefolje/statustall")
    public EnhetPortefoljeStatustallRespons hentEnhetPortefoljeStatustall(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return new EnhetPortefoljeStatustallRespons(
                opensearchService.hentStatusTallForEnhetPortefolje(enhet, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ),
                opensearchService.hentStatusTallForEnhetPortefolje(enhet, BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ)
        );
    }

    @GetMapping("/{enhet}/tiltak")
    public EnhetTiltak hentTiltak(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return tiltakService.hentEnhettiltak(EnhetId.of(enhet));
    }

    @GetMapping("/{enhet}/foedeland")
    public List<Foedeland> hentFoedeland(
            @PathVariable("enhet")
            String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return tryCacheFirst(enhetFoedelandCache, enhet,
                () -> personOpprinnelseService.hentFoedeland(enhet));
    }

    @GetMapping("/{enhet}/tolkSpraak")
    public List<TolkSpraak> hentTolkSpraak(
            @PathVariable("enhet")
            String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return tryCacheFirst(enhetTolkSpraakCache, enhet,
                () -> personOpprinnelseService.hentTolkSpraak(enhet));
    }


    @GetMapping("/{enhet}/geografiskbosted")
    public List<GeografiskBosted> hentGeografiskBosted(
            @PathVariable("enhet")
            String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return tryCacheFirst(enhetGeografiskBostedCache, enhet,
                () -> bostedService.hentGeografiskBosted(enhet));
    }
}
