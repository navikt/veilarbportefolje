package no.nav.pto.veilarbportefolje.dialog;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogService extends KafkaCommonConsumerService<Dialogdata> {
    private final DialogRepository dialogRepository;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final DialogRepositoryV2 dialogRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(Dialogdata melding) {
        dialogRepository.oppdaterDialogInfoForBruker(melding);
        dialogRepositoryV2.oppdaterDialogInfoForBruker(melding);

        log.info("Oppdatert dialog for bruker: {} med 'venter på svar fra NAV': {}, 'venter på svar fra bruker': {}, sist endret: {}", melding.getAktorId(), toIsoUTC(melding.getTidspunktEldsteUbehandlede()), toIsoUTC(melding.getTidspunktEldsteVentende()), melding.getSisteEndring());
        opensearchIndexerV2.updateDialog(melding);
    }

    public String migrerDialogData() {
        List<String> aktorIds = dialogRepository.hentBrukereMedGamleAktiveDialoger();
        int antallFeilet = 0;
        for (var aktorId : aktorIds) {
            try {
                Try<Dialogdata> dialogdata = dialogRepository.retrieveDialogData(aktorId);
                dialogRepositoryV2.oppdaterDialogInfoForBruker(dialogdata.get());
            } catch (Exception e) {
                log.warn("feilet migrering av aktoer: {}", aktorId, e);
                antallFeilet += 1;
            }
        }
        return "Antall feilet: " + antallFeilet;
    }
}
