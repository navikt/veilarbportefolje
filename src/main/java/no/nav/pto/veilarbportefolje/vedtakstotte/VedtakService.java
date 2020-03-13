package no.nav.pto.veilarbportefolje.vedtakstotte;

import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import org.elasticsearch.common.xcontent.XContentBuilder;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


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

    @SneakyThrows
    private void slettUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettVedtakUtkast(melding.getVedtakId());
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();

        byggVedtakstotteNullVerdiJson()
                .map(json -> new Tuple2<>(fnr, json))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s, %s ", melding.getAktorId(), error))));
    }

    private void oppdaterUtkast(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.upsertVedtak(melding);
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();

        byggVedtakstotteJson(melding)
                .map(json -> new Tuple2<>(fnr, json))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s i brukerindeks, %s ", melding.getAktorId(), error))));
    }


    private void setUtkastTilSendt(KafkaVedtakStatusEndring melding) {
        vedtakStatusRepository.slettGamleVedtakOgUtkast(melding.getAktorId());
        vedtakStatusRepository.upsertVedtak(melding);
        Fnr fnr = aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())).get();

        byggVedtakstotteNullVerdiJson()
                .map(doc -> new Tuple2<>(fnr, doc))
                .map(tuple -> elasticIndexer.oppdaterBruker(tuple)
                        .onFailure(error -> log.warn(String.format("Feil ved oppdatering i brukerindeks av bruker med aktoerId: %s . %s ", melding.getAktorId(), error))));
    }

    private Try<XContentBuilder> byggVedtakstotteNullVerdiJson() {
        return Try.of(() ->
                jsonBuilder()
                        .startObject()
                        .nullField("vedtak_status")
                        .nullField("vedtak_status_endret")
                        .endObject());
    }


    private Try<XContentBuilder> byggVedtakstotteJson(KafkaVedtakStatusEndring melding) {
        return Try.of(() ->
                jsonBuilder()
                        .startObject()
                        .field("vedtak_status", melding.getVedtakStatus().name())
                        .field("vedtak_status_endret", melding.getStatusEndretTidspunkt().toString())
                        .endObject());
    }

}
