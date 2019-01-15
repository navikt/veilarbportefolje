package no.nav.fo.veilarbportefolje.indeksering;

import io.micrometer.core.instrument.LongTaskTimer;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakHandler;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.veilarbportefolje.service.KrrService;
import no.nav.metrics.MetricsFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;

@Slf4j
public class IndekseringScheduler {

    private final LongTaskTimer timer;

    @Inject
    private IndekseringService indekseringService;

    @Inject
    private TiltakHandler tiltakHandler;

    @Inject
    private KopierGR199FraArena kopierGR199FraArena;

    @Inject
    private KrrService krrService;

    public IndekseringScheduler() {
        timer = LongTaskTimer.builder("indeksering_total").register(MetricsFactory.getMeterRegistry());
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void totalIndexering() {
        timer.record(() -> {
            kopierGR199FraArena.startOppdateringAvYtelser();
            tiltakHandler.startOppdateringAvTiltakIDatabasen();
            krrService.hentDigitalKontaktInformasjonBolk();
            indekseringService.hovedindeksering();

        });
    }

    @Scheduled(cron = "0 * * * * *")
    public void deltaindeksering() {
        indekseringService.deltaindeksering();
    }

}
