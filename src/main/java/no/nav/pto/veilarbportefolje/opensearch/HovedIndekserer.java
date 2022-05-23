package no.nav.pto.veilarbportefolje.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukAvAliasIndeksering;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.hentIdenterFraPostgres;

@Slf4j
@Service
@RequiredArgsConstructor
public class HovedIndekserer {
    private final OpensearchIndexer opensearchIndexer;
    private final OpensearchAdminService opensearchAdminService;
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final UnleashService unleashService;

    @SneakyThrows
    public void hovedIndeksering() {
        log.info("Starter jobb: hovedindeksering");
        List<AktorId> brukereSomMaOppdateres;
        if (hentIdenterFraPostgres(unleashService)) {
            brukereSomMaOppdateres = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();
        } else {
            brukereSomMaOppdateres = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        }

        if (brukAvAliasIndeksering(unleashService)) {
            aliasBasertHovedIndeksering(brukereSomMaOppdateres);
        } else {
            opensearchIndexer.oppdaterAlleBrukereIOpensearch(brukereSomMaOppdateres);
        }
    }

    @SneakyThrows
    private void aliasBasertHovedIndeksering(List<AktorId> brukere) {
        long tidsStempel0 = System.currentTimeMillis();
        log.info("Hovedindeksering: Indekserer {} brukere", brukere.size());

        String gammelIndex = opensearchAdminService.hentBrukerIndex();
        String nyIndex = opensearchAdminService.opprettSkjultSkriveIndeksPaAlias();
        log.info("Hovedindeksering: skaper 'write index': {}", nyIndex);

        boolean success = tryIndekserAlleBrukere(brukere);
        if (success) {
            opensearchAdminService.flyttAliasTilNyIndeks(gammelIndex, nyIndex);
            opensearchAdminService.slettIndex(gammelIndex);
            long tid = System.currentTimeMillis() - tidsStempel0;
            log.info("Hovedindeksering: Ferdig på {} ms, indekserte {} brukere", tid, brukere.size());
        } else {
            opensearchAdminService.slettIndex(nyIndex);
            log.error("Hovedindeksering: ble ikke fullført");
        }
    }

    private boolean tryIndekserAlleBrukere(List<AktorId> brukere) {
        try {
            return opensearchIndexer.indexerInParallel(brukere);
        } catch (Exception e) {
            log.error("Hovedindeksering: feilet...", e);
            return false;
        }
    }
}
