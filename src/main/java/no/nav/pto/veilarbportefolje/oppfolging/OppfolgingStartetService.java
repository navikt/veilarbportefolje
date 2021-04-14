package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OppfolgingStartetService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticIndexer elasticIndexer;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final OppfolgingStartetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingStartetDTO.class);
        oppfolgingRepository.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        oppfolgingRepositoryV2.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        elasticIndexer.indekser(dto.getAktorId());
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
