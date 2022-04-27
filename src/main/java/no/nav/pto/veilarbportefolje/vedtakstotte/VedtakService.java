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
public class VedtakService extends KafkaCommonConsumerService<KafkaVedtakStatusEndring> {
    private final VedtakStatusRepositoryV2 vedtakStatusRepositoryV2;
    private final OpensearchIndexer opensearchIndexer;

    @Override
    public void behandleKafkaMeldingLogikk(KafkaVedtakStatusEndring vedtakStatusEndring) {
        KafkaVedtakStatusEndring.VedtakStatusEndring vedtakStatus = vedtakStatusEndring.getVedtakStatusEndring();
        switch (vedtakStatus) {
            case UTKAST_SLETTET, VEDTAK_SENDT -> slettUtkast(vedtakStatusEndring);
            case UTKAST_OPPRETTET -> opprettUtkast(vedtakStatusEndring);
            case OVERTA_FOR_VEILEDER -> oppdaterAnsvarligVeileder(vedtakStatusEndring);
            case BESLUTTER_PROSESS_STARTET, BESLUTTER_PROSESS_AVBRUTT, BLI_BESLUTTER,
                    GODKJENT_AV_BESLUTTER, KLAR_TIL_BESLUTTER, KLAR_TIL_VEILEDER -> oppdaterUtkast(vedtakStatusEndring);
        }
        opensearchIndexer.indekser(AktorId.of(vedtakStatusEndring.getAktorId()));
    }

    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepositoryV2.slettGamleVedtakOgUtkast(melding.getAktorId());
    }

    private void opprettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepositoryV2.upsertVedtak(melding);
        log.info("Opprettet/oppdatert vedtaksutkast med ID: {} for bruker: {}", melding.getVedtakId(), melding.aktorId);
    }

    private void oppdaterAnsvarligVeileder(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepositoryV2.oppdaterAnsvarligVeileder(melding);
    }

    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepositoryV2.updateVedtak(melding);
    }
}
