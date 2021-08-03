package no.nav.pto.veilarbportefolje.registrering;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class RegistreringService implements KafkaConsumerService<ArbeidssokerRegistrertEvent> {
    private final RegistreringRepository registreringRepository;
    private final ElasticServiceV2 elastic;
    private final AtomicBoolean rewind;

    @Autowired
    public RegistreringService(RegistreringRepository registreringRepository, ElasticServiceV2 elastic) {
        this.registreringRepository = registreringRepository;
        this.elastic = elastic;
        this.rewind = new AtomicBoolean();
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        registreringRepository.upsertBrukerRegistrering(kafkaRegistreringMelding);

        final AktorId aktoerId = AktorId.of(kafkaRegistreringMelding.getAktorid());
        elastic.updateRegistering(aktoerId, kafkaRegistreringMelding);
    }

    public void slettRegistering(AktorId aktoerId) {
        registreringRepository.slettBrukerRegistrering(aktoerId);
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
