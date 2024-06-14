package no.nav.pto.veilarbportefolje.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Enhet", description = "Portefølje-funksjonalitet på enhetsnivå.")
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

    @Operation(
            summary = "Hent portefølje for enhet",
            description = "Henter en liste med brukere under arbeidsrettet oppfølging knyttet til enheten."
    )
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
        authService.innloggetVeilederHarTilgangTilOppfolging();
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(enhet, Optional.empty(), sortDirection, sortField, filtervalg, fra, antall);
        List<Bruker> sensurerteBrukereSublist = authService.sensurerBrukere(brukereMedAntall.getBrukere());

        return PortefoljeUtils.buildPortefolje(brukereMedAntall.getAntall(),
                sensurerteBrukereSublist,
                enhet,
                Optional.ofNullable(fra).orElse(0));
    }

    @Operation(
            summary = "Hent porteføljestørrelser for enhet",
            description = "Henter antall brukere i porteføljen for en gitt enhet."
    )
    @GetMapping("/{enhet}/portefoljestorrelser")
    public FacetResults hentPortefoljestorrelser(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return opensearchService.hentPortefoljestorrelser(enhet);
    }

    @Operation(
            summary = "Hent statustall for enhet portefølje",
            description = "Henter statustall på enhetsnivå (statistikk for alle brukere under arbeidsrettet oppfølging tilknyttet enheten), delt opp i brukere som veileder som utfører forespørselen har innsynsrett på og brukere som veileder ikke har innsynsrett på."
    )
    @GetMapping("/{enhet}/portefolje/statustall")
    public EnhetPortefoljeStatustallRespons hentEnhetPortefoljeStatustall(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return new EnhetPortefoljeStatustallRespons(
                opensearchService.hentStatusTallForEnhetPortefolje(enhet, BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ),
                opensearchService.hentStatusTallForEnhetPortefolje(enhet, BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ)
        );
    }

    @Operation(
            summary = "Hent tiltak for enhet",
            description = "Henter alle tiltakstyper for enheten hvor minst en bruker er tilknyttet tiltaket."
    )
    @GetMapping("/{enhet}/tiltak")
    public EnhetTiltak hentTiltak(@PathVariable("enhet") String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return tiltakService.hentEnhettiltak(EnhetId.of(enhet));
    }

    @Operation(
            summary = "Hent fødeland for enhet",
            description = "Henter en liste av fødeland for brukere på enheten. Listen inneholder kun land som er registrert på brukere på enheten."
    )
    @GetMapping("/{enhet}/foedeland")
    public List<Foedeland> hentFoedeland(
            @PathVariable("enhet")
            String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return tryCacheFirst(enhetFoedelandCache, enhet,
                () -> personOpprinnelseService.hentFoedeland(enhet));
    }

    @Operation(
            summary = "Hent språk med tolkebehov for enhet",
            description = "Henter en liste av språk for enheten hvor det er tolkebehov. Listen inneholder kun språk som er registrert på brukere på enheten."
    )
    @GetMapping("/{enhet}/tolkSpraak")
    public List<TolkSpraak> hentTolkSpraak(
            @PathVariable("enhet")
            String enhet) {
        ValideringsRegler.sjekkEnhet(enhet);
        authService.innloggetVeilederHarTilgangTilEnhet(enhet);

        return tryCacheFirst(enhetTolkSpraakCache, enhet,
                () -> personOpprinnelseService.hentTolkSpraak(enhet));
    }


    @Operation(
            summary = "Hent geografisk bosted for enhet",
            description = "Henter en liste av geografiske bosteder (kommuner og bydeler) for enheten. Listen inneholder kun geografiske bosteder som er registrert på brukere på enheten."
    )
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
