package no.nav.pto.veilarbportefolje.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.opensearch.HovedIndekserer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

@Slf4j
@Configuration
public class SchedulConfig {
    private final HovedIndekserer hovedIndekserer;
    private final AktivitetService aktivitetService;
    private final YtelsesService ytelsesService;
    private final Scheduler scheduler;

    @Autowired
    public SchedulConfig(@Qualifier("Postgres") DataSource dataSource,
                         HovedIndekserer hovedIndekserer,
                         AktivitetService aktivitetService,
                         YtelsesService ytelsesService) {
        this.hovedIndekserer = hovedIndekserer;
        this.aktivitetService = aktivitetService;
        this.ytelsesService = ytelsesService;

        List<RecurringTask<?>> jobber = nattligeJobber();
        scheduler = Scheduler
                .create(dataSource)
                .deleteUnresolvedAfter(Duration.of(1, HOURS))
                .startTasks(jobber)
                .registerShutdownHook()
                .build();
    }

    // Disse jobben må kjøre etter midnatt
    private List<RecurringTask<?>> nattligeJobber() {
        return List.of(Tasks.recurring("indekserer_aktivitet_endringer", Schedules.daily(LocalTime.of(1, 1)))
                        .execute((instance, ctx) -> hovedIndekserer.hovedIndeksering()),
                Tasks.recurring("indekserer_ytelse_endringer", Schedules.daily(LocalTime.of(2, 0)))
                        .execute((instance, ctx) -> ytelsesService.oppdaterBrukereMedYtelserSomStarterIDag()),
                Tasks.recurring("deaktiver_utgatte_utdannings_aktivteter", Schedules.daily(LocalTime.of(2, 0)))
                        .execute((instance, ctx) -> aktivitetService.deaktiverUtgatteUtdanningsAktivteter())
        );
    }

    @PostConstruct
    public void start() {
        scheduler.start();
    }
}
