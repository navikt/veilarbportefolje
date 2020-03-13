package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;


@Slf4j
public class VedtakService implements KafkaConsumerService {

    private VedtakStatusRepository vedtakStatusRepository;
    private ElasticIndexer elasticIndexer;
    public static final String KAFKA_VEDTAK_CONSUMER_TOPIC = "aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName();

    public VedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        this.elasticIndexer = elasticIndexer;
    }

    public void behandleKafkaMelding(String kafkaMelding, String kafkaTopic) {
        if (kafkaTopic.equals(KAFKA_VEDTAK_CONSUMER_TOPIC)) {
            log.info("Behandler melding for på topic: " + KAFKA_VEDTAK_CONSUMER_TOPIC);
            KafkaVedtakStatusEndring melding = fromJson(kafkaMelding, KafkaVedtakStatusEndring.class);
            KafkaVedtakStatusEndring.KafkaVedtakStatus vedtakStatus = melding.getVedtakStatus();
            switch (vedtakStatus) {
                case UTKAST_SLETTET:
                    vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
                    break;
                case UTKAST_OPPRETTET:
                case SENDT_TIL_BESLUTTER:
                    vedtakStatusRepository.upsertVedtak(melding);
                    break;
                case SENDT_TIL_BRUKER:
                    vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
                    vedtakStatusRepository.upsertVedtak(melding);
                    break;
            }
            elasticIndexer.indekser(AktoerId.of(melding.getAktorId()));
        }
    }
}
