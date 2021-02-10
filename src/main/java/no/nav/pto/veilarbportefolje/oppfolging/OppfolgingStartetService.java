package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.pdldata.PdlDataService;
import org.springframework.stereotype.Service;

@Service
public class OppfolgingStartetService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final PdlDataService pdlDataService;

    public OppfolgingStartetService(OppfolgingRepository oppfolgingRepository, PdlDataService pdlDataService) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.pdlDataService = pdlDataService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final OppfolgingStartetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingStartetDTO.class);
        oppfolgingRepository.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());

        pdlDataService.inititerBruker(dto.getAktorId());
    }

    @Override
    public boolean shouldRewind() {
        return false;
    }

    @Override
    public void setRewind(boolean rewind) {

    }
}
