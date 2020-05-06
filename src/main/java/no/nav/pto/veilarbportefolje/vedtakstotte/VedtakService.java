package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;

import static no.nav.json.JsonUtils.fromJson;

@Slf4j
public class VedtakService implements KafkaConsumerService {

    private VedtakStatusRepository vedtakStatusRepository;
    private ElasticIndexer elasticIndexer;
    private AktoerService aktoerService;

    public VedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
    }

    public void behandleKafkaMelding(String melding) {
        KafkaVedtakStatusEndring vedtakStatusEndring = fromJson(melding, KafkaVedtakStatusEndring.class);
        KafkaVedtakStatusEndring.VedtakStatusEndring vedtakStatus = vedtakStatusEndring.getVedtakStatusEndring();
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
            case GODKJENT_AV_BESLUTTER:
            case KLAR_TIL_BESLUTTER:
            case KLAR_TIL_VEILEDER: {
                oppdaterUtkast(vedtakStatusEndring);
            }
        }
    }

    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        indekserBruker(AktoerId.of(melding.getAktorId()));
    }


    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
        indekserBruker(AktoerId.of(melding.getAktorId()));
    }


    private void setVedtakSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
        indekserBruker(AktoerId.of(melding.getAktorId()));
    }

    private void indekserBruker (AktoerId aktoerId) {
        try {
            elasticIndexer.indekser(aktoerId);
        } catch (NullPointerException e) {
            aktoerService.hentFnrFraAktorId(aktoerId)
                    .onSuccess(elasticIndexer::indekser);
        }
    }
}
