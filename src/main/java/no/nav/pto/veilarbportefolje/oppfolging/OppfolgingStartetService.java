package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingStartetService {
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;
    private final PdlService pdlService;
    private final Siste14aVedtakService siste14aVedtakService;
    private final UnleashService unleashService;

    public void startOppfolging(AktorId aktorId, ZonedDateTime oppfolgingStartetDate) {
        pdlService.hentOgLagrePdlData(aktorId);

        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartetDate);

        if(FeatureToggle.hentSiste14aVedtakVedOppfolgingStartet(unleashService)) {
            siste14aVedtakService.hentOgLagreSiste14aVedtak(aktorId);
        }

        opensearchIndexer.indekser(aktorId);
        log.info("Bruker {} har startet oppfølging: {}", aktorId, oppfolgingStartetDate);
    }
}
