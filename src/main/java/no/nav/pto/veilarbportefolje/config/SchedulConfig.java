package no.nav.pto.veilarbportefolje.config;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.JobRunner;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesService;
import no.nav.pto.veilarbportefolje.arenapakafka.ytelser.YtelsesServicePostgres;
import no.nav.pto.veilarbportefolje.database.BrukerAktiviteterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SchedulConfig {
    private final BrukerAktiviteterService brukerAktiviteterService;
    private final YtelsesService ytelsesService;
    private final YtelsesServicePostgres ytelsesServicePostgres;

    // Denne jobben må kjøre etter midnatt
    @Bean
    public Task<Void> oppdaterBrukerAktiviteter() {
        return Tasks.recurring("indekserer_aktivitet_endringer", Schedules.daily(LocalTime.of(0, 1)))
                .execute((instance, ctx) -> JobRunner.run("indekserer_aktivitet_endringer", brukerAktiviteterService::syncAktivitetOgBrukerData));
    }

    // Denne jobben må kjøre etter midnatt
    @Bean
    public Task<Void> oppdaterNyeYtelser() {
        return Tasks.recurring("indekserer_ytelse_endringer", Schedules.daily(LocalTime.of(1, 0)))
                .execute((instance, ctx) -> JobRunner.run("indekserer_ytelse_endringer", ytelsesService::oppdaterBrukereMedYtelserSomStarterIDagOracle));
    }

    // Denne jobben må kjøre etter midnatt
    @Bean
    public Task<Void> oppdaterNyeYtelserPostgres() {
        return Tasks.recurring("indekserer_ytelse_endringer_postgres", Schedules.daily(LocalTime.of(1, 1)))
                .execute((instance, ctx) -> JobRunner.run("indekserer_ytelse_endringer_postgres", ytelsesServicePostgres::oppdaterBrukereMedYtelserSomStarterIDagPostgres));
    }
}
