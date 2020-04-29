package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;

import static no.nav.json.JsonUtils.fromJson;

@Slf4j
public class VedtakService implements KafkaConsumerService {

    private VedtakStatusRepository vedtakStatusRepository;
    private ElasticIndexer elasticIndexer;

    public VedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        this.elasticIndexer = elasticIndexer;
    }

    public void behandleKafkaMelding(String melding) {
        log.info("Behandler melding {} " + melding);
        KafkaVedtakStatusEndring vedtakStatusEndring = fromJson(melding, KafkaVedtakStatusEndring.class);
        KafkaVedtakStatusEndring.VedtakStatusEndring vedtakStatus = vedtakStatusEndring.getVedtakStatusEndring();
        log.info("Behandler vedtakstatus {} " + vedtakStatus.name());
        switch (vedtakStatus) {
            case UTKAST_SLETTET : {
                slettUtkast(vedtakStatusEndring);
                return;
            }
            case VEDTAK_SENDT: {
                setVedtakSendt(vedtakStatusEndring);
                return;
            }
            case UTKAST_OPPRETTET:
            case BESLUTTER_PROSESS_STARTET:
            case BLI_BESLUTTER:
            case OVERTA_FOR_BESLUTTER:
            case OVERTA_FOR_VEILEDER:
            case GODKJENT_AV_BESLUTTER:
            case KLAR_TIL_BESLUTTER:
            case KLAR_TIL_VEILEDER: {
                log.info("oppdaterUtkast vedtakstatus {} " + vedtakStatus.name());
                oppdaterUtkast(vedtakStatusEndring);
            }
        }
    }

    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        elasticIndexer.indekserAsynkront(AktoerId.of(melding.getAktorId()));
    }


    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
        elasticIndexer.indekserAsynkront(AktoerId.of(melding.getAktorId()));
    }


    private void setVedtakSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
        elasticIndexer.indekserAsynkront(AktoerId.of(melding.getAktorId()));
    }
}
