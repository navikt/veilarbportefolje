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

import static no.nav.json.JsonUtils.fromJson;

@Slf4j
public class DialogService implements KafkaConsumerService<String> {

    private final DialogFeedRepository dialogFeedRepository;
    private final ElasticIndexer elasticIndexer;
    private final AktoerService aktoerService;
    private final BrukerRepository brukerRepository;


    public DialogService(DialogFeedRepository dialogFeedRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService, BrukerRepository brukerRepository) {
        this.dialogFeedRepository = dialogFeedRepository;
        this.elasticIndexer = elasticIndexer;
        this.aktoerService = aktoerService;
        this.brukerRepository = brukerRepository;
    }

    @Override
    public Result<String> behandleKafkaMelding(String kafkaMelding) {
        DialogData melding = fromJson(kafkaMelding, DialogData.class);
        Result<Fnr> result = Result.of(() -> {
            dialogFeedRepository.oppdaterDialogInfoForBruker(melding);
            return aktoerService.hentFnrFraAktorId(AktoerId.of(melding.getAktorId()))
                    .getOrElseThrow(() -> new IllegalStateException());
        });

        return result
                .andThen(this::indekserBruker)
                .map(b -> b.getAktoer_id());
    }

    private Result<OppfolgingsBruker> indekserBruker(Fnr fnr) {
        Result<OppfolgingsBruker> oppfolgingsBrukerResult = brukerRepository.hentBruker(fnr);
        if (UnderOppfolgingRegler.erUnderOppfolging(oppfolgingsBrukerResult)) {
            return elasticIndexer.indekser(fnr);
        }
        return Result.err("Indeksering feilet");
    }

}
