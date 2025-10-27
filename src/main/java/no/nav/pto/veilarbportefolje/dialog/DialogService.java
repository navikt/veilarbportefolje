package no.nav.pto.veilarbportefolje.dialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService extends KafkaCommonNonKeyedConsumerService<Dialogdata> {
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;
    private final DialogRepositoryV2 dialogRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(Dialogdata melding) {
        dialogRepositoryV2.oppdaterDialogInfoForBruker(melding);

        secureLog.info("Oppdatert dialog for bruker: {} med 'venter på svar fra Nav': {}, 'venter på svar fra bruker': {}, sist endret: {}", melding.getAktorId(), toIsoUTC(melding.getTidspunktEldsteUbehandlede()), toIsoUTC(melding.getTidspunktEldsteVentende()), melding.getSisteEndring());
        opensearchIndexerPaDatafelt.updateDialog(melding);
    }
}
