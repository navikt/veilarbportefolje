package no.nav.pto.veilarbportefolje.sistelest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SistLestService extends KafkaCommonConsumerService<SistLestKafkaMelding> {
    private final BrukerServiceV2 brukerService;
    private final SisteEndringService sisteEndringService;

    public void behandleKafkaMeldingLogikk(SistLestKafkaMelding melding) {
        log.info("Aktivitetsplanen for {} ble lest av {}, lest: {}", melding.getAktorId(), melding.getVeilederId(), melding.getHarLestTidspunkt());
        Optional<VeilederId> veilederId = brukerService.hentVeilederForBruker(melding.getAktorId());
        if (veilederId.isEmpty()) {
            return;
        }
        if (veilederId.get().equals(melding.getVeilederId())) {
            sisteEndringService.veilederHarSett(melding.getAktorId(), melding.getHarLestTidspunkt());
        }
    }
}
