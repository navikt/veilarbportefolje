package no.nav.pto.veilarbportefolje.opensearch;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.brukAvAliasIndeksering;
import static no.nav.pto.veilarbportefolje.util.DateUtils.now;

@Slf4j
@Service
@RequiredArgsConstructor
public class HovedIndekserer implements MeterBinder {
    private final OpensearchIndexer opensearchIndexer;
    private final OpensearchAdminService opensearchAdminService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final UnleashService unleashService;

    private final AtomicLong sisteHovedindekseringTimestamp;
    private final AtomicLong sisteHovedindekseringDuration;


    public void hovedIndeksering() {
        log.info("Starter jobb: hovedindeksering");
        List<AktorId> brukereSomMaOppdateres;
        brukereSomMaOppdateres = oppfolgingRepositoryV2.hentAlleGyldigeBrukereUnderOppfolging();

        if (brukAvAliasIndeksering(unleashService)) {
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
            sisteHovedindekseringTimestamp.set(now().toInstant().toEpochMilli());
            sisteHovedindekseringDuration.set(tid);
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

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        Gauge.builder("veilarbportefolje.hovedindeksering.siste_kjorte", this::sisteHovedindeksering)
                .register(meterRegistry);
        Gauge.builder("veilarbportefolje.hovedindeksering.duration", this::getSisteHovedindekseringDuration)
                .register(meterRegistry);
    }

    private long sisteHovedindeksering() {
        if (sisteHovedindekseringTimestamp != null) {
            return sisteHovedindekseringTimestamp.get();
        }
        return 0L;
    }

    private long getSisteHovedindekseringDuration() {
        if (sisteHovedindekseringDuration != null) {
            return sisteHovedindekseringDuration.get();
        }
        return 0L;
    }
}
