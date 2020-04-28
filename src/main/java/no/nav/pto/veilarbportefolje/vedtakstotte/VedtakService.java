package no.nav.pto.veilarbportefolje.vedtakstotte;

import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
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
                oppdaterUtkast(vedtakStatusEndring);
            }
        }
    }

    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        nullstillVedtakStatusIIndeks(melding);
    }


    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
        oppdaterVedtaksStatusIIndeks(melding);
    }


    private void setVedtakSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
        nullstillVedtakStatusIIndeks(melding);
    }

    private void oppdaterVedtaksStatusIIndeks(KafkaVedtakStatusEndring melding) {
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();
        VedtakUtils.byggVedtakstotteJson(melding)
                .map(json -> new Tuple2<>(fnr, json))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s i brukerindeks, %s ", melding.getAktorId(), error))));
    }

    private void nullstillVedtakStatusIIndeks(KafkaVedtakStatusEndring melding) {
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();
        VedtakUtils.byggVedtakstotteNullVerdiJson()
                .map(json -> new Tuple2<>(fnr, json))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s, %s ", melding.getAktorId(), error))));
    }

}
