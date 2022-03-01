package no.nav.pto.veilarbportefolje.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesServicePostgres;
import no.nav.pto.veilarbportefolje.database.BrukerAktiviteterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Configuration
public class SchedulConfig {
    private final BrukerAktiviteterService brukerAktiviteterService;
    private final YtelsesService ytelsesService;
    private final YtelsesServicePostgres ytelsesServicePostgres;
    private final Scheduler scheduler;

    @Autowired
    public SchedulConfig(@Qualifier("Postgres") DataSource dataSource,
                         BrukerAktiviteterService brukerAktiviteterService,
                         YtelsesServicePostgres ytelsesServicePostgres,
                         YtelsesService ytelsesService) {
        this.brukerAktiviteterService = brukerAktiviteterService;
        this.ytelsesServicePostgres = ytelsesServicePostgres;
        this.ytelsesService = ytelsesService;

        List<RecurringTask<?>> jobber = Stream.concat(nattligeJobber().stream(), test().stream()).toList();
        scheduler = Scheduler
                .create(dataSource)
                .startTasks(jobber)
                .registerShutdownHook()
                .build();
    }

    private List<RecurringTask<?>> nattligeJobber() {
        return List.of(Tasks.recurring("indekserer_aktivitet_endringer", Schedules.daily(LocalTime.of(1, 1)))
                        .execute((instance, ctx) -> brukerAktiviteterService.syncAktivitetOgBrukerData()),
                Tasks.recurring("indekserer_ytelse_endringer", Schedules.daily(LocalTime.of(2, 0)))
                        .execute((instance, ctx) -> ytelsesService.oppdaterBrukereMedYtelserSomStarterIDagOracle()),
                Tasks.recurring("indekserer_ytelse_endringer_postgres", Schedules.daily(LocalTime.of(2, 1)))
                        .execute((instance, ctx) -> ytelsesServicePostgres.oppdaterBrukereMedYtelserSomStarterIDagPostgres())
        );
    }

    private List<RecurringTask<?>> test() {
        return List.of(Tasks.recurring("cron_test1", Schedules.cron("*/10 * * * * ?"))
                        .execute((instance, ctx) -> log.info("cron1 test")),
                Tasks.recurring("cron_test2", Schedules.cron("*/15 * * * * ?"))
                        .execute((instance, ctx) -> log.info("cron2 test")));
    }

    @PostConstruct
    public void start() {
        scheduler.start();
    }
}
