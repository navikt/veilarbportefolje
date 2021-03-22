package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.pdldata.PdlDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OppfolgingStartetService implements KafkaConsumerService<String> {

    private final OppfolgingRepository oppfolgingRepository;
    private final PdlDataService pdlDataService;
    private final ElasticIndexer elasticIndexer;

    @Autowired
    public OppfolgingStartetService(OppfolgingRepository oppfolgingRepository, PdlDataService pdlDataService, ElasticIndexer elasticIndexer) {
        this.oppfolgingRepository = oppfolgingRepository;
        this.pdlDataService = pdlDataService;
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        final OppfolgingStartetDTO dto = JsonUtils.fromJson(kafkaMelding, OppfolgingStartetDTO.class);
        oppfolgingRepository.settUnderOppfolging(dto.getAktorId(), dto.getOppfolgingStartet());

        pdlDataService.lastInnPdlData(dto.getAktorId());
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
