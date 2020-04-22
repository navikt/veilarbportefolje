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
        VedtakStatusEndring vedtakStatusEndring = fromJson(melding, VedtakStatusEndring.class);
        VedtakStatusEndring.KafkaVedtakStatus vedtakStatus = vedtakStatusEndring.getVedtakStatus();
        switch (vedtakStatus) {
            case UTKAST_SLETTET : {
                slettUtkast(vedtakStatusEndring);
                return;
            }
            case UTKAST_OPPRETTET:
            case SENDT_TIL_BESLUTTER: {
                oppdaterUtkast(vedtakStatusEndring);
                return;
            }
            case SENDT_TIL_BRUKER: {
                setUtkastTilSendt(vedtakStatusEndring);
            }
        }
    }

    private void slettUtkast(VedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        skrivNullFeldterTilIndeks(melding);
    }


    private void oppdaterUtkast(VedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
        skrivOppdateringTilIndeks(melding);
    }


    private void setUtkastTilSendt(VedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
        skrivNullFeldterTilIndeks(melding);
    }

    private void skrivOppdateringTilIndeks(VedtakStatusEndring melding) {
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();
        VedtakUtils.byggVedtakstotteJson(melding)
                .map(json -> new Tuple2<>(fnr, json))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s i brukerindeks, %s ", melding.getAktorId(), error))));
    }

    private void skrivNullFeldterTilIndeks(VedtakStatusEndring melding) {
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();
        VedtakUtils.byggVedtakstotteNullVerdiJson()
                .map(json -> new Tuple2<>(fnr, json))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s, %s ", melding.getAktorId(), error))));
    }

}
