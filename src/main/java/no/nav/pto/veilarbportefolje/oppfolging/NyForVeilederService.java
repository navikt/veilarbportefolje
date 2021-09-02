package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;

@Service
@RequiredArgsConstructor
public class NyForVeilederService extends KafkaCommonConsumerService<NyForVeilederDTO> implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final ElasticServiceV2 elasticServiceV2;
    @Getter
    private final UnleashService unleashService;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {

        final NyForVeilederDTO dto = JsonUtils.fromJson(kafkaMelding, NyForVeilederDTO.class);
        behandleKafkaMeldingLogikk(dto);
    }

    @Override
    protected void behandleKafkaMeldingLogikk(NyForVeilederDTO dto) {
        final boolean brukerErNyForVeileder = dto.isNyForVeileder();
        oppfolgingRepository.settNyForVeileder(dto.getAktorId(), brukerErNyForVeileder);
        if (erPostgresPa(unleashService)) {
            oppfolgingRepositoryV2.settNyForVeileder(dto.getAktorId(), brukerErNyForVeileder);
        }

        elasticServiceV2.oppdaterNyForVeileder(dto.getAktorId(), brukerErNyForVeileder);
    }

    @Override
    public boolean shouldRewind() {
        return rewind.get();
    }

    @Override
    public void setRewind(boolean rewind) {
        this.rewind.set(rewind);
    }

}
