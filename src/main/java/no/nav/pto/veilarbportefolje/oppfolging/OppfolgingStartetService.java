package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OppfolgingStartetService {
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;

    public void startOppfolging(AktorId aktorId, ZonedDateTime oppfolgingStartetDate) {
        oppfolgingRepository.settUnderOppfolging(aktorId, oppfolgingStartetDate);
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartetDate);
        opensearchIndexer.indekser(aktorId);
        log.info("Bruker {} har startet oppfølging: {}", aktorId, oppfolgingStartetDate);
    }
}
