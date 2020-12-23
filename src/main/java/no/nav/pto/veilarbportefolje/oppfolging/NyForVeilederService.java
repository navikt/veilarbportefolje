package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.KAFKA_OPPFOLGING;

@Service
public class NyForVeilederService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final UnleashService unleashService;

    public NyForVeilederService(OppfolgingRepository oppfolgingRepository, ElasticServiceV2 elasticServiceV2, UnleashService unleashService) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.elasticServiceV2 = elasticServiceV2;
        this.unleashService = unleashService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        if (!unleashService.isEnabled(KAFKA_OPPFOLGING)) {
            return;
        }
        final NyForVeilederDTO dto = JsonUtils.fromJson(kafkaMelding, NyForVeilederDTO.class);

        final boolean brukerIkkeErNyForVeileder = !dto.isNyForVeileder();
        if (brukerIkkeErNyForVeileder) {
            oppfolgingRepository.settNyForVeileder(dto.getAktorId(), false);
            elasticServiceV2.oppdaterNyForVeileder(dto.getAktorId(), false);
        }else{
            oppfolgingRepository.settNyForVeileder(dto.getAktorId(), true);
            elasticServiceV2.oppdaterNyForVeileder(dto.getAktorId(), true);
        }
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
