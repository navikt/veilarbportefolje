package no.nav.pto.veilarbportefolje.dialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService extends KafkaCommonConsumerService<Dialogdata> implements KafkaConsumerService<String> {

    private final DialogRepository dialogRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final DialogRepositoryV2 dialogRepositoryV2;
    private final UnleashService unleashService;
    private final AtomicBoolean rewind = new AtomicBoolean();

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        if (isNyKafkaLibraryEnabled()) {
            return;
        }
        Dialogdata melding = fromJson(kafkaMelding, Dialogdata.class);
        behandleKafkaMeldingLogikk(melding);
    }

    @Override
    protected void behandleKafkaMeldingLogikk(Dialogdata melding) {
        dialogRepository.oppdaterDialogInfoForBruker(melding);
        if (erPostgresPa(unleashService)) {
            int rader = dialogRepositoryV2.oppdaterDialogInfoForBruker(melding);
            log.info("Oppdatert dialog for bruker: {}, i postgres rader pavirket: {}", melding.getAktorId(), rader);
        }

        elasticServiceV2.updateDialog(melding);
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
