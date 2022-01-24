package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingStartetService extends KafkaCommonConsumerService<OppfolgingStartetDTO> {

    private final OppfolgingRepository oppfolgingRepository;
    private final OpensearchIndexer opensearchIndexer;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(OppfolgingStartetDTO dto) {
        oppfolgingRepository.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        oppfolgingRepositoryV2.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        opensearchIndexer.indekser(dto.getAktorId());
        log.info("Bruker {} har startet oppfølging: {}", dto.getAktorId(), dto.getOppfolgingStartet());
    }
}
