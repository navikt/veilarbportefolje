package no.nav.pto.veilarbportefolje.opensearch;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukAvAliasIndeksering;

@Slf4j
@Service
@RequiredArgsConstructor
public class HovedIndekserer {
    private final OpensearchIndexer opensearchIndexer;
    private final OpensearchAdminService opensearchAdminService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final DefaultUnleash defaultUnleash;

    public void hovedIndeksering() {
        log.info("Starter jobb: hovedindeksering");
        List<AktorId> brukereSomMaOppdateres;
        brukereSomMaOppdateres = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();

        if (brukAvAliasIndeksering(defaultUnleash)) {
            aliasBasertHovedIndeksering(brukereSomMaOppdateres);
        } else {
            opensearchIndexer.oppdaterAlleBrukereIOpensearch(brukereSomMaOppdateres);
        }
    }

    public void aliasBasertHovedIndeksering(List<AktorId> brukere) {
        long tidsStempel0 = System.currentTimeMillis();
        log.info("Hovedindeksering: Indekserer {} brukere", brukere.size());

        String gammelIndex = opensearchAdminService.hentBrukerIndex();
        String nyIndex = opensearchAdminService.opprettSkjultSkriveIndeksPaAlias();
        log.info("Hovedindeksering: skaper 'write index': {}", nyIndex);

        boolean success = tryIndekserAlleBrukere(brukere);
        if (success) {
            opensearchAdminService.slettGammeltOgOppdaterNyttAlias(gammelIndex, nyIndex);
            opensearchAdminService.slettIndex(gammelIndex);
            long tid = System.currentTimeMillis() - tidsStempel0;
            log.info("Hovedindeksering: Ferdig på {} ms, indekserte {} brukere", tid, brukere.size());
        } else {
            opensearchAdminService.slettIndex(nyIndex);
            throw new RuntimeException("Hovedindeksering: ble ikke fullført");
        }
    }

    private boolean tryIndekserAlleBrukere(List<AktorId> brukere) {
        try {
            opensearchIndexer.batchIndeksering(brukere);
            return true;
        } catch (Exception e) {
            log.error("Hovedindeksering feilet", e);
            return false;
        }
    }
}
