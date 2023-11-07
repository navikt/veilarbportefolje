package no.nav.pto.veilarbportefolje.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.opensearch.HovedIndekserer;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

@Slf4j
@Configuration
public class SchedulConfig {
    private final HovedIndekserer hovedIndekserer;
    private final AktivitetService aktivitetService;
    private final YtelsesService ytelsesService;

    private final BarnUnder18AarService barnUnder18AarService;
    private final Scheduler scheduler;

    public static String deaktiverUtgatteUtdanningsAktivteter = "deaktiver_utgatte_utdannings_aktivteter";
    public static String indeksererYtelseEndringer = "indekserer_ytelse_endringer";
    public static String indeksererAktivitetEndringer = "indekserer_aktivitet_endringer";

    public static String slettDataForBarnSomErOver18 = "slett_data_for_barn_som_er_over_18";

    @Autowired
    public SchedulConfig(DataSource dataSource,
                         HovedIndekserer hovedIndekserer,
                         AktivitetService aktivitetService,
                         YtelsesService ytelsesService, BarnUnder18AarService barnUnder18AarService) {
        this.hovedIndekserer = hovedIndekserer;
        this.aktivitetService = aktivitetService;
        this.ytelsesService = ytelsesService;
        this.barnUnder18AarService = barnUnder18AarService;

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
        return List.of(
                Tasks.recurring(deaktiverUtgatteUtdanningsAktivteter, Schedules.daily(LocalTime.of(2, 1)))
                        .execute((instance, ctx) -> aktivitetService.deaktiverUtgatteUtdanningsAktivteter()),
                Tasks.recurring(indeksererYtelseEndringer, Schedules.daily(LocalTime.of(2, 1)))
                        .execute((instance, ctx) -> ytelsesService.oppdaterBrukereMedYtelserSomStarterIDag()),
                Tasks.recurring(indeksererAktivitetEndringer, Schedules.daily(LocalTime.of(2, 15)))
                        .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(3, (executionComplete, executionOperations) -> {
                            log.error("Hovedindeksering har feilet {} ganger. Forsøker igjen om 5 min",
                                    executionComplete.getExecution().consecutiveFailures + 1);
                            executionOperations.reschedule(executionComplete, Instant.now().plus(5, MINUTES));
                        }))
                        .execute((instance, ctx) -> hovedIndekserer.hovedIndeksering()),
                Tasks.recurring(slettDataForBarnSomErOver18, Schedules.daily(LocalTime.of(1, 1)))
                        .execute((instance, ctx) -> barnUnder18AarService.slettDataForBarnSomErOver18())
        );
    }

    @PostConstruct
    public void start() {
        scheduler.start();
    }
}
