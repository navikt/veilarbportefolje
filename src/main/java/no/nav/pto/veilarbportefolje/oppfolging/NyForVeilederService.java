package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class NyForVeilederService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    public NyForVeilederService(OppfolgingRepository oppfolgingRepository, ElasticServiceV2 elasticServiceV2) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.elasticServiceV2 = elasticServiceV2;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final NyForVeilederDTO dto = JsonUtils.fromJson(kafkaMelding, NyForVeilederDTO.class);

        final boolean brukerIkkeErNyForVeileder = !dto.isNyForVeileder();
        if (brukerIkkeErNyForVeileder) {
            oppfolgingRepository.settNyForVeileder(dto.getAktorId(), false);
            elasticServiceV2.oppdaterNyForVeileder(dto.getAktorId(), false);
        }
        // Vi trenger ikke å opppdatere db/indeks når bruker er ny for veileder, siden dette gjøres i VeilederTilordnetService
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
