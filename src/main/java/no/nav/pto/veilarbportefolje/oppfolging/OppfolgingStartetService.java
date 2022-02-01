package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OppfolgingStartetService extends KafkaCommonConsumerService<OppfolgingStartetDTO> {
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;
    private final BrukerRepository brukerRepository;
    private final AktorClient aktorClient;

    @Override
    public void behandleKafkaMeldingLogikk(OppfolgingStartetDTO dto) {
        mapAktoerTilPersonId(dto.getAktorId());

        oppfolgingRepository.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        oppfolgingRepositoryV2.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        opensearchIndexer.indekser(dto.getAktorId());
        log.info("Bruker {} har startet oppfølging: {}", dto.getAktorId(), dto.getOppfolgingStartet());
    }

    private void mapAktoerTilPersonId(AktorId aktorId) {
        List<PersonId> mappedePersonIder = brukerRepository.hentMappedePersonIder(aktorId);

        if (mappedePersonIder.size() == 0) {
            brukerRepository.retrievePersonidFromFnr(aktorClient.hentFnr(aktorId))
                    .ifPresentOrElse(
                            personId -> {
                                log.info("Mapper aktorId: {}, til personId: {}", aktorId, personId);
                                brukerRepository.setGjeldeneFlaggTilNull(personId);
                                brukerRepository.insertAktoeridToPersonidMapping(aktorId, personId);
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
