package no.nav.pto.veilarbportefolje.sistelest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;

@Service
@Slf4j
@RequiredArgsConstructor
public class SistLestService implements KafkaConsumerService<String> {
    private final SistLestRepository sistLestRepository;
    private final BrukerService brukerService;
    private final ElasticServiceV2 elasticServiceV2;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        SistLestKafkaMelding melding = fromJson(kafkaMelding, SistLestKafkaMelding.class);
        log.info("Aktivitetsplanen for {} ble lest av {}, lest: {}", melding.getAktorId(), melding.getVeilederId(), melding.getHarLestTidspunkt());
        Optional<VeilederId> veilederId = brukerService.hentVeilederForBruker(melding.getAktorId());
        if(veilederId.isEmpty()){
            return;
        }
        if (veilederId.get().equals(melding.getVeilederId())) {
            sistLestRepository.upsert(melding);
            elasticServiceV2.updateSistLest(melding);
        }
    }

    public void slettSistLest(AktorId aktorId) {
        sistLestRepository.slettSistLest(aktorId);
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
