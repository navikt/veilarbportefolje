package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.opensearch.HovedIndekserer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukAvAliasIndeksering;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerAktiviteterService {
    private final AktivitetService aktivitetService;
    private final OppfolgingRepository oppfolgingRepository;
    private final OpensearchIndexer opensearchIndexer;
    private final HovedIndekserer hovedIndekserer;
    private final UnleashService unleashService;

    public void syncAktivitetOgBrukerData() {
        log.info("Starter jobb: oppdater BrukerAktiviteter og BrukerData");
        List<AktorId> brukereSomMaOppdateres = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        aktivitetService.deaktiverUtgatteUtdanningsAktivteter();

        if (brukAvAliasIndeksering(unleashService)) {
            hovedIndekserer.hovedIndeksering(brukereSomMaOppdateres);
        } else {
            opensearchIndexer.oppdaterAlleBrukereIOpensearch(brukereSomMaOppdateres);
        }
    }
}
