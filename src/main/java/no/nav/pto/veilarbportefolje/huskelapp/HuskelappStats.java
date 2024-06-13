package no.nav.pto.veilarbportefolje.huskelapp;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class HuskelappStats implements MeterBinder {

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate jdbcTemplate;

    private final LeaderElectionClient leaderElection;

    private MultiGauge huskelapp_stats;
    private MultiGauge arbeidsliste_stats;

    @Override
    public void bindTo(@NonNull MeterRegistry meterRegistry) {
        if (leaderElection.isLeader()) {
            huskelapp_stats = MultiGauge.builder("huskelapp_antall")
                    .description("The number of active huskelapper")
                    .register(meterRegistry);

            arbeidsliste_stats = MultiGauge.builder("arbeidsliste_antall")
                    .description("The number of active arbeidsliste")
                    .register(meterRegistry);
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void oppdaterHuskelappMetrikk() {
        try {
            if (leaderElection.isLeader()) {
                String query = String.format("select %s, count(*) as huskelapp_antall from %s where %s = 'AKTIV' group by %s", PostgresTable.HUSKELAPP.ENHET_ID, PostgresTable.HUSKELAPP.TABLE_NAME, PostgresTable.HUSKELAPP.STATUS, PostgresTable.HUSKELAPP.ENHET_ID);
                Map<String, Integer> huskelappAntall = this.jdbcTemplate.query(query, rs -> {
                            Map<String, Integer> stats = new HashMap<>();
                            while (rs.next()) {
                                stats.put(rs.getString(PostgresTable.HUSKELAPP.ENHET_ID), rs.getInt("huskelapp_antall"));
                            }
                            return stats;
                        }
                );
                if (huskelappAntall != null && huskelapp_stats != null) {
                    huskelapp_stats.register(huskelappAntall.entrySet().stream().map(entry -> MultiGauge.Row.of(Tags.of("enhetId", entry.getKey()), entry.getValue())).collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            log.error("Can not fetch huskelapp metrics " + e, e);
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void oppdaterArbeidslisteMetrikk() {
        try {
            if (leaderElection.isLeader()) {
                String query = String.format("select %s, count(*) as arbeidsliste_antall from %s group by %s;", PostgresTable.ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE, PostgresTable.ARBEIDSLISTE.TABLE_NAME, PostgresTable.ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE);
                Map<String, Integer> arbeidslisteAntall = this.jdbcTemplate.query(query, rs -> {
                            Map<String, Integer> stats = new HashMap<>();
                            while (rs.next()) {
                                stats.put(rs.getString(PostgresTable.ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE), rs.getInt("arbeidsliste_antall"));
                            }
                            return stats;
                        }
                );
                if (arbeidslisteAntall != null && arbeidsliste_stats != null) {
                    arbeidsliste_stats.register(arbeidslisteAntall.entrySet().stream().map(entry -> MultiGauge.Row.of(Tags.of("enhetId", entry.getKey()), entry.getValue())).collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            log.error("Can not fetch huskelapp and arbeidsliste metrics " + e, e);
        }
    }
}
