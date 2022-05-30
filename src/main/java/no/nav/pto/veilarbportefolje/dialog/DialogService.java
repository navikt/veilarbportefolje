package no.nav.pto.veilarbportefolje.dialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService extends KafkaCommonConsumerService<Dialogdata> {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final DialogRepositoryV2 dialogRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(Dialogdata melding) {
        dialogRepositoryV2.oppdaterDialogInfoForBruker(melding);

        log.info("Oppdatert dialog for bruker: {} med 'venter på svar fra NAV': {}, 'venter på svar fra bruker': {}, sist endret: {}", melding.getAktorId(), toIsoUTC(melding.getTidspunktEldsteUbehandlede()), toIsoUTC(melding.getTidspunktEldsteVentende()), melding.getSisteEndring());
        opensearchIndexerV2.updateDialog(melding);
    }
}
