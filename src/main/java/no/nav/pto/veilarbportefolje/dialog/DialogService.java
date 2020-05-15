package no.nav.pto.veilarbportefolje.dialog;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.Result;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.metrics.utils.MetricsUtils.timed;

@Slf4j
public class DialogService implements KafkaConsumerService {

    private DialogFeedRepository dialogFeedRepository;
    private ElasticIndexer elasticIndexer;
    private AktoerService aktoerService;


    public DialogService(DialogFeedRepository dialogFeedRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService) {
        this.dialogFeedRepository = dialogFeedRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        timed("portefolje.dialogservice.performace",()-> {
            DialogData melding = fromJson(kafkaMelding, DialogData.class);
            dialogFeedRepository.oppdaterDialogInfoForBruker(melding);
            indekserBruker(AktoerId.of(melding.getAktorId()));
        });
    }

    private void indekserBruker (AktoerId aktoerId) {
        Result<OppfolgingsBruker> result = elasticIndexer.indekser(aktoerId)
                .mapError(err -> {
                            Fnr fnr = aktoerService.hentFnrFraAktorId(aktoerId).get();
                            return elasticIndexer.indekser(fnr);
                        }
                );

        if(result.isErr()) {
            log.warn("Feil ved indeksering av bruker med aktorId {}", aktoerId.aktoerId);
        }

    }
}
