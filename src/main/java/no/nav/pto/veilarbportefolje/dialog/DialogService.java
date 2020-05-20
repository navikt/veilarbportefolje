package no.nav.pto.veilarbportefolje.dialog;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;

import java.util.concurrent.CompletableFuture;

import static no.nav.json.JsonUtils.fromJson;

@Slf4j
public class DialogService implements KafkaConsumerService<String> {

    private DialogFeedRepository dialogFeedRepository;
    private ElasticIndexer elasticIndexer;
    private AktoerService aktoerService;
    private BrukerRepository brukerRepository;


    public DialogService(DialogFeedRepository dialogFeedRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService, BrukerRepository brukerRepository) {
        this.dialogFeedRepository = dialogFeedRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
        this.brukerRepository = brukerRepository;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        long startTime = System.currentTimeMillis();
        DialogData melding = fromJson(kafkaMelding, DialogData.class);
        CompletableFuture.runAsync(()-> dialogFeedRepository.oppdaterDialogInfoForBruker(melding));
        CompletableFuture.supplyAsync(()-> aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId())))
                .thenAccept(tryFnr -> tryFnr.onSuccess(this::indekserBruker));
        long endTime = System.currentTimeMillis();
        log.info("Dialog service async tog {} millisekunder att exekvera", (startTime-endTime));
    }

    private void indekserBruker (Fnr fnr) {
        Result<OppfolgingsBruker> oppfolgingsBrukerResult = brukerRepository.hentBruker(fnr);
        if(UnderOppfolgingRegler.erUnderOppfolging(oppfolgingsBrukerResult)) {
            elasticIndexer.indekser(fnr);
        }
    }
}
