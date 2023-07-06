package no.nav.pto.veilarbportefolje.internal;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Component
@RequiredArgsConstructor
public class BrukerMappingAlarm implements MeterBinder {
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate db;

    private final AtomicInteger antallBrukereSomIkkeHarIdentIPDL = new AtomicInteger(0);
    private final AtomicInteger antallAktiveBrukereSomIkkeHarBrukerDataFraPDL = new AtomicInteger(0);
    private final AtomicInteger antallBrukereSomIkkeLiggerIDatabaseLenkenFraArena = new AtomicInteger(0);

    @Override
    public void bindTo(@NonNull MeterRegistry meterRegistry) {
        Gauge.builder("veilarbportefolje_ikke_mappet_bruker_ident", antallBrukereSomIkkeHarIdentIPDL, AtomicInteger::get)
                .description("Antall brukere under oppfølging som ikke har en ident lagret i tabellen: bruker_identer")
                .register(meterRegistry);
        Gauge.builder("veilarbportefolje_ikke_mappet_bruker_brukerdata", antallAktiveBrukereSomIkkeHarBrukerDataFraPDL, AtomicInteger::get)
                .description("Antall brukere under oppfølging som ikke har en bruker data lagret i tabellen: bruker_data")
                .register(meterRegistry);
        Gauge.builder("veilarbportefolje_ikke_mappet_bruker_databaselenke", antallBrukereSomIkkeLiggerIDatabaseLenkenFraArena, AtomicInteger::get)
                .description("Antall brukere under oppfølging som ikke har en bruker data lagret i tabellen: oppfolgingsbruker_arena_v2")
                .register(meterRegistry);
    }

    @Scheduled(cron = "* /10 * * * ?")
    public void oppdaterMetrikk() {
        antallBrukereSomIkkeHarIdentIPDL.set(antallBrukereSomIkkeHarIdentIPDL());
        antallAktiveBrukereSomIkkeHarBrukerDataFraPDL.set(antallAktiveBrukereSomIkkeHarBrukerDataFraPDL());
        antallBrukereSomIkkeLiggerIDatabaseLenkenFraArena.set(antallBrukereSomIkkeLiggerIDatabaseLenkenFraArena());
    }
    private int antallBrukereSomIkkeHarIdentIPDL() {
        String sql = """
                select count(*) from oppfolging_data od
                    left join bruker_identer bi on bi.ident = od.aktoerid
                    where bi is null;
                """;
        return Optional.ofNullable(
                queryForObjectOrNull(
                        () -> db.queryForObject(sql, Integer.class))
        ).orElse(0);
    }

    private int antallAktiveBrukereSomIkkeHarBrukerDataFraPDL() {
        String sql = """
                select count(*) from oppfolging_data od
                    inner join aktive_identer ai on ai.aktorid = od.aktoerid
                    left join bruker_data bd on bd.freg_ident = ai.fnr
                    where bd is null;
                """;
        return Optional.ofNullable(
                queryForObjectOrNull(
                        () -> db.queryForObject(sql, Integer.class))
        ).orElse(0);
    }

    private int antallBrukereSomIkkeLiggerIDatabaseLenkenFraArena() {
        String sql = """
                select count(*) from
                    (select bi.person from bruker_identer bi
                        left join oppfolging_data od on od.aktoerid = bi.ident
                        left join oppfolgingsbruker_arena_v2 ob on ob.fodselsnr = bi.ident
                        group by bi.person
                        having count(ob.*) = 0 and count(od.*) > 0
                    ) as personer;
                """;
        return Optional.ofNullable(
                queryForObjectOrNull(
                        () -> db.queryForObject(sql, Integer.class))
        ).orElse(0);
    }
}