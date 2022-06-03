package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Utkast14aStatusendringService extends KafkaCommonConsumerService<Kafka14aStatusendring> {
    private final Utkast14aStatusRepository utkast14aStatusRepository;
    private final OpensearchIndexer opensearchIndexer;

    @Override
    public void behandleKafkaMeldingLogikk(Kafka14aStatusendring statusEndring) {
        Kafka14aStatusendring.Status status = statusEndring.getVedtakStatusEndring();
        switch (status) {
            case UTKAST_SLETTET, VEDTAK_SENDT -> slettUtkast(statusEndring);
            case UTKAST_OPPRETTET -> opprettUtkast(statusEndring);
            case OVERTA_FOR_VEILEDER -> oppdaterAnsvarligVeileder(statusEndring);
            case BESLUTTER_PROSESS_STARTET, BESLUTTER_PROSESS_AVBRUTT, BLI_BESLUTTER,
                    GODKJENT_AV_BESLUTTER, KLAR_TIL_BESLUTTER, KLAR_TIL_VEILEDER -> oppdaterUtkast(statusEndring);
        }
        opensearchIndexer.indekser(AktorId.of(statusEndring.getAktorId()));
    }

    private void slettUtkast(Kafka14aStatusendring melding) {
        utkast14aStatusRepository.slettUtkastForBruker(melding.getAktorId());
    }

    private void opprettUtkast(Kafka14aStatusendring melding) {
        utkast14aStatusRepository.upsert(melding);
        log.info("Opprettet/oppdatert utkast 14a status med ID: {} for bruker: {}", melding.getVedtakId(), melding.aktorId);
    }

    private void oppdaterAnsvarligVeileder(Kafka14aStatusendring melding) {
        utkast14aStatusRepository.oppdaterAnsvarligVeileder(melding);
    }

    private void oppdaterUtkast(Kafka14aStatusendring melding) {
        utkast14aStatusRepository.update(melding);
    }
}
