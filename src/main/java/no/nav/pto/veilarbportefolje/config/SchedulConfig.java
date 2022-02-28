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

        scheduler = Scheduler.create(dataSource,
                        oppdaterBrukerAktiviteter(), oppdaterNyeYtelser(),
                        oppdaterNyeYtelserPostgres(), test())
                .build();
    }

    // Denne jobben må kjøre etter midnatt
    private RecurringTask<Void> oppdaterBrukerAktiviteter() {
        return Tasks.recurring("indekserer_aktivitet_endringer", Schedules.daily(LocalTime.of(0, 1)))
                .execute((instance, ctx) -> brukerAktiviteterService.syncAktivitetOgBrukerData());
    }

    // Denne jobben må kjøre etter midnatt
    private RecurringTask<Void> oppdaterNyeYtelser() {
        return Tasks.recurring("indekserer_ytelse_endringer", Schedules.daily(LocalTime.of(1, 0)))
                .execute((instance, ctx) -> ytelsesService.oppdaterBrukereMedYtelserSomStarterIDagOracle());
    }

    // Denne jobben må kjøre etter midnatt
    private RecurringTask<Void> oppdaterNyeYtelserPostgres() {
        return Tasks.recurring("indekserer_ytelse_endringer_postgres", Schedules.daily(LocalTime.of(1, 1)))
                .execute((instance, ctx) -> ytelsesServicePostgres.oppdaterBrukereMedYtelserSomStarterIDagPostgres());
    }

    private RecurringTask<Void> test() {
        return Tasks.recurring("test", Schedules.daily(LocalTime.of(16, 30)))
                .execute((instance, ctx) -> log.info("hello world: db-schedule"));
    }

    @PostConstruct
    public void start() {
        scheduler.start();
        log.info("Starting... Scheduler state: {}", scheduler.getSchedulerState());
    }
}
