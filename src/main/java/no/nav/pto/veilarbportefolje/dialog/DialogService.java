package no.nav.pto.veilarbportefolje.dialog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService extends KafkaCommonConsumerService<Dialogdata> {

    private final DialogRepository dialogRepository;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final DialogRepositoryV2 dialogRepositoryV2;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(Dialogdata melding) {
        dialogRepository.oppdaterDialogInfoForBruker(melding);
        dialogRepositoryV2.oppdaterDialogInfoForBruker(melding);

        log.info("Oppdatert dialog for bruker: {} med 'venter på svar fra NAV': {}, 'venter på svar fra bruker': {}, sist endret: {}", melding.getAktorId(), toIsoUTC(melding.getTidspunktEldsteUbehandlede()), toIsoUTC(melding.getTidspunktEldsteVentende()), melding.getSisteEndring());
        if (oppfolgingRepositoryV2.erUnderOppfolging(AktorId.of(melding.aktorId))) {
            opensearchIndexerV2.updateDialog(melding);
        }
    }
}
