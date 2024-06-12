package no.nav.pto.veilarbportefolje.huskelapp;

import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.concurrent.ConcurrentHashMap;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.HUSKELAPP;

@Component
@RequiredArgsConstructor
@Slf4j
public class HuskelappStats implements MeterBinder {

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate jdbcTemplate;

    private final Map<String, Integer> huskelappStats = new ConcurrentHashMap<>();

    @Override
    public void bindTo(@NonNull MeterRegistry meterRegistry) {
        log.info("Reporting metrics for huskelapp " + this.huskelappStats.keySet().size());
        this.huskelappStats.keySet().forEach(enhet_id -> {
            log.info("Reporting metrics for huskelapp antall " + enhet_id);
            meterRegistry.gauge("huskelapp_antall", Tags.of("enhet_id", enhet_id),
                    this.huskelappStats.get(enhet_id)
            );
        });
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void oppdaterMetrikk() {
        try {
            String query = String.format("select %s, count(*) as huskelapp_antall from %s where %s = 'AKTIV' group by %s", HUSKELAPP.ENHET_ID, HUSKELAPP.TABLE_NAME, HUSKELAPP.STATUS, HUSKELAPP.ENHET_ID);
            Map<String, Integer> huskelappAntall = this.jdbcTemplate.queryForObject(query, (rs, rowNum) -> {
                        Map<String, Integer> stats = new HashMap<>();
                        while (rs.next()) {
                            stats.put(rs.getString(HUSKELAPP.ENHET_ID), rs.getInt("huskelapp_antall"));
                        }
                        return stats;
                    }
            );
            if (huskelappAntall != null) {
                this.huskelappStats.clear();
                this.huskelappStats.putAll(huskelappAntall);
            }
        } catch (Exception e) {
            log.error("Can not fetch huskelapp metrics " + e, e);
        }
    }
}
