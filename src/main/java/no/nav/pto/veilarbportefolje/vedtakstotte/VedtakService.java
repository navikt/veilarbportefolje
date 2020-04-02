package no.nav.pto.veilarbportefolje.vedtakstotte;

import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.AktoerService;

@Slf4j
public class VedtakService {

    private VedtakStatusRepository vedtakStatusRepository;
    private ElasticIndexer elasticIndexer;
    private AktoerService aktoerService;

    public VedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
    }

    public void behandleMelding(KafkaVedtakStatusEndring melding) {
        KafkaVedtakStatusEndring.KafkaVedtakStatus vedtakStatus = melding.getVedtakStatus();
        switch (vedtakStatus) {
            case UTKAST_SLETTET : {
                slettUtkast(melding);
                return;
            }
            case UTKAST_OPPRETTET:
            case SENDT_TIL_BESLUTTER: {
                oppdaterUtkast(melding);
                return;
            }
            case SENDT_TIL_BRUKER: {
                setUtkastTilSendt(melding);
            }
        }
    }

    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        skrivNullFeldterTilIndeks(melding);
    }


    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
        skrivOppdateringTilIndeks(melding);
    }


    private void setUtkastTilSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
        skrivNullFeldterTilIndeks(melding);
    }

    private void skrivOppdateringTilIndeks(KafkaVedtakStatusEndring melding) {
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();
        VedtakUtils.byggVedtakstotteJson(melding)
                .map(json -> new Tuple2<>(fnr, json))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s i brukerindeks, %s ", melding.getAktorId(), error))));
    }

    private void skrivNullFeldterTilIndeks(KafkaVedtakStatusEndring melding) {
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();
        VedtakUtils.byggVedtakstotteNullVerdiJson()
                .map(json -> new Tuple2<>(fnr, json))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s, %s ", melding.getAktorId(), error))));
    }

}
