package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.KAFKA_OPPFOLGING;

@Service
public class OppfolgingStartetService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final UnleashService unleashService;

    public OppfolgingStartetService(OppfolgingRepository oppfolgingRepository, UnleashService unleashService) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.unleashService = unleashService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        if (!unleashService.isEnabled(KAFKA_OPPFOLGING)) {
            return;
        }
        final OppfolgingStartetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingStartetDTO.class);
        oppfolgingRepository.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
