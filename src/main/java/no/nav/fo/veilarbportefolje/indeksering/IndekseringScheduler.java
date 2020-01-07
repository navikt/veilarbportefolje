package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakHandler;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.veilarbportefolje.service.KrrService;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

import static no.nav.fo.veilarbportefolje.batchjob.BatchJob.runAsyncJobOnLeader;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;


@Slf4j
public class IndekseringScheduler {

    private ElasticIndexer elasticIndexer;

    private TiltakHandler tiltakHandler;

    private KopierGR199FraArena kopierGR199FraArena;

    private KrrService krrService;

    private final Counter totalIndekseringCounter;

    private final Counter deltaIndekseringCounter;

    @Inject
    public IndekseringScheduler(ElasticIndexer elasticIndexer, TiltakHandler tiltakHandler, KopierGR199FraArena kopierGR199FraArena, KrrService krrService) {
        this.elasticIndexer = elasticIndexer;
        this.tiltakHandler = tiltakHandler;
        this.kopierGR199FraArena = kopierGR199FraArena;
        this.krrService = krrService;
        this.totalIndekseringCounter = Counter.builder("portefolje_totalindeksering_feilet").register(getMeterRegistry());
        this.deltaIndekseringCounter = Counter.builder("portefolje_deltaindeksering_feilet").register(getMeterRegistry());
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void totalIndexering() {
        runAsyncJobOnLeader(
                () -> {
                    try {
                        kopierGR199FraArena.startOppdateringAvYtelser();
                        tiltakHandler.startOppdateringAvTiltakIDatabasen();
                        krrService.hentDigitalKontaktInformasjonBolk();
                    } finally {
                        elasticIndexer.startIndeksering();
                    }
                }
                , totalIndekseringCounter
        );
    }

    @Scheduled(cron = "0 * * * * *")
    public void deltaindeksering() {
        runAsyncJobOnLeader(elasticIndexer::deltaindeksering, deltaIndekseringCounter);
    }

}
