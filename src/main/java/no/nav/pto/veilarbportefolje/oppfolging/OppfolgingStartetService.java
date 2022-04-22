package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OppfolgingStartetService {
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;
    private final BrukerRepository brukerRepository;
    private final AktorClient aktorClient;
    private final PdlService pdlService;

    public void startOppfolging(AktorId aktorId, ZonedDateTime oppfolgingStartetDate) {
        mapAktoerTilPersonId(aktorId);

        pdlService.hentOgLagreIdenter(aktorId);

        oppfolgingRepository.settUnderOppfolging(aktorId, oppfolgingStartetDate);
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, oppfolgingStartetDate);
        opensearchIndexer.indekser(aktorId);
        log.info("Bruker {} har startet oppfølging: {}", aktorId, oppfolgingStartetDate);
    }

    private void mapAktoerTilPersonId(AktorId aktorId) {
        List<PersonId> mappedePersonIder = brukerRepository.hentMappedePersonIder(aktorId);

        if (mappedePersonIder.size() == 0) {
            brukerRepository.retrievePersonidFromFnr(aktorClient.hentFnr(aktorId))
                    .ifPresentOrElse(
                            personId -> {
                                log.info("Mapper aktorId: {}, til personId: {}", aktorId, personId);
                                brukerRepository.setGjeldeneFlaggTilNull(personId);
                                brukerRepository.upsertAktoeridToPersonidMapping(aktorId, personId);
                            },
                            () -> {
                                // Kaster exception for å utnytte retry-mekanisme i KafkaConsumerClient
                                throw new NoSuchElementException("Det finnes ingen personId i DB link for aktorId: " + aktorId);
                            }
                    );
        } else if (mappedePersonIder.size() > 1) {
            log.error("Det var flere mappet en personId for aktoer: {}, personIder: {}", aktorId.get(), mappedePersonIder);
        }
    }
}
