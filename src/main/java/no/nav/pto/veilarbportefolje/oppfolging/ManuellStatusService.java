package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.ManuellBrukerStatus;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;

@Service
@RequiredArgsConstructor
public class ManuellStatusService implements KafkaConsumerService<String> {
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ElasticServiceV2 elasticServiceV2;
    private final UnleashService unleashService;

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final ManuellStatusDTO dto = JsonUtils.fromJson(kafkaMelding, ManuellStatusDTO.class);
        final AktorId aktoerId = AktorId.of(dto.getAktorId());

        oppfolgingRepository.settManuellStatus(aktoerId, dto.isErManuell());
        if (erPostgresPa(unleashService)) {
            oppfolgingRepositoryV2.settManuellStatus(aktoerId, dto.isErManuell());
        }
        String manuellStatus = dto.isErManuell() ? ManuellBrukerStatus.MANUELL.name() : null;

        elasticServiceV2.settManuellStatus(aktoerId, manuellStatus);
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }

}
