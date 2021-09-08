package no.nav.pto.veilarbportefolje.sistelest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;

@Service
@Slf4j
@RequiredArgsConstructor
public class SistLestService extends KafkaCommonConsumerService<SistLestKafkaMelding> implements KafkaConsumerService<String> {
    private final BrukerService brukerService;
    private final SisteEndringService sisteEndringService;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        SistLestKafkaMelding melding = fromJson(kafkaMelding, SistLestKafkaMelding.class);
        behandleKafkaMeldingLogikk(melding);
    }

    protected void behandleKafkaMeldingLogikk(SistLestKafkaMelding melding) {
        log.info("Aktivitetsplanen for {} ble lest av {}, lest: {}", melding.getAktorId(), melding.getVeilederId(), melding.getHarLestTidspunkt());
        Optional<VeilederId> veilederId = brukerService.hentVeilederForBruker(melding.getAktorId());
        if (veilederId.isEmpty()) {
            return;
        }
        if (veilederId.get().equals(melding.getVeilederId())) {
            sisteEndringService.veilederHarSett(melding.getAktorId(), melding.getHarLestTidspunkt());
        }
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
