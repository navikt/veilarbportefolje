package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;

@Service
@RequiredArgsConstructor
public class OppfolgingStartetService extends KafkaCommonConsumerService<OppfolgingStartetDTO> implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticIndexer elasticIndexer;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    @Getter
    private final UnleashService unleashService;

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        if (isNyKafkaLibraryEnabled()) {
            return;
        }
        final OppfolgingStartetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingStartetDTO.class);
        behandleKafkaMeldingLogikk(dto);
    }

    @Override
    public void behandleKafkaMeldingLogikk(OppfolgingStartetDTO dto) {
        oppfolgingRepository.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        if (erPostgresPa(unleashService)) {
            oppfolgingRepositoryV2.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
        }
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
