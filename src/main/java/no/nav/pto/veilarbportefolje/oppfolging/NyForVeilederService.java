package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class NyForVeilederService extends KafkaCommonConsumerService<NyForVeilederDTO> {

    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ElasticServiceV2 elasticServiceV2;

    @Override
    protected void behandleKafkaMeldingLogikk(NyForVeilederDTO dto) {
        final boolean brukerErNyForVeileder = dto.isNyForVeileder();
        oppfolgingRepository.settNyForVeileder(dto.getAktorId(), brukerErNyForVeileder);
        oppfolgingRepositoryV2.settNyForVeileder(dto.getAktorId(), brukerErNyForVeileder);

        elasticServiceV2.oppdaterNyForVeileder(dto.getAktorId(), brukerErNyForVeileder);
        log.info("Oppdatert bruker: {}, er ny for veileder: {}", dto.getAktorId(), brukerErNyForVeileder);
    }
}
