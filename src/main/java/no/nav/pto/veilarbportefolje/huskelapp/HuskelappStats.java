package no.nav.pto.veilarbportefolje.huskelapp;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSLISTE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.HUSKELAPP;

@Component
@RequiredArgsConstructor
@Slf4j
public class HuskelappStats implements MeterBinder {

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate jdbcTemplate;

    private MultiGauge huskelapp_stats;
    private MultiGauge arbeidsliste_stats;

    @Override
    public void bindTo(@NonNull MeterRegistry meterRegistry) {
        huskelapp_stats = MultiGauge.builder("huskelapp_antall")
                .description("The number of active huskelapper")
                .register(meterRegistry);

        arbeidsliste_stats = MultiGauge.builder("arbeidsliste_antall")
                .description("The number of active arbeidsliste")
                .register(meterRegistry);
    }

    @Scheduled(cron = "0 */2 * * * *")
    public void oppdaterHuskelappMetrikk() {
        try {
            String query = String.format("select %s, count(*) as huskelapp_antall from %s where %s = 'AKTIV' group by %s", HUSKELAPP.ENHET_ID, HUSKELAPP.TABLE_NAME, HUSKELAPP.STATUS, HUSKELAPP.ENHET_ID);
            Map<String, Integer> huskelappAntall = this.jdbcTemplate.query(query, rs -> {
                        Map<String, Integer> stats = new HashMap<>();
                        while (rs.next()) {
                            stats.put(rs.getString(HUSKELAPP.ENHET_ID), rs.getInt("huskelapp_antall"));
                        }
                        return stats;
                    }
            );
            if (huskelappAntall != null && huskelapp_stats != null) {
                huskelapp_stats.register(huskelappAntall.entrySet().stream().map(entry -> MultiGauge.Row.of(Tags.of("enhetId", entry.getKey()), entry.getValue())).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.error("Can not fetch huskelapp metrics " + e, e);
        }
    }

    @Scheduled(cron = "0 */2 * * * *")
    public void oppdaterArbeidslisteMetrikk() {
        try {
            String query = String.format("select %s, count(*) as arbeidsliste_antall from %s group by %s;", ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE, ARBEIDSLISTE.TABLE_NAME, ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE);
            Map<String, Integer> arbeidslisteAntall = this.jdbcTemplate.query(query, rs -> {
                        Map<String, Integer> stats = new HashMap<>();
                        while (rs.next()) {
                            stats.put(rs.getString(ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE), rs.getInt("arbeidsliste_antall"));
                        }
                        return stats;
                    }
            );
            if (arbeidslisteAntall != null && arbeidsliste_stats != null) {
                arbeidsliste_stats.register(arbeidslisteAntall.entrySet().stream().map(entry -> MultiGauge.Row.of(Tags.of("enhetId", entry.getKey()), entry.getValue())).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.error("Can not fetch huskelapp and arbeidsliste metrics " + e, e);
        }
    }
}
