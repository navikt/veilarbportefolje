package no.nav.pto.veilarbportefolje.internal;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Component
@RequiredArgsConstructor
public class BrukerMappingAlarm implements MeterBinder {
    private final JdbcTemplate db;

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        Gauge.builder("veilarbportefolje_ikke_mappet_bruker_ident", this::antallBrukereSomIkkeHarIdentIPDL)
                .description("Antall brukere under oppfølging som ikke har en ident lagret i tabellen: bruker_identer")
                .register(meterRegistry);
        Gauge.builder("veilarbportefolje_ikke_mappet_bruker_brukerdata", this::antallAktiveBrukereSomIkkeHarBrukerDataFraPDL)
                .description("Antall brukere under oppfølging som ikke har en bruker data lagret i tabellen: bruker_data")
                .register(meterRegistry);
        Gauge.builder("veilarbportefolje_ikke_mappet_bruker_databaselenke", this::antallBrukereSomIkkeLiggerIDatabaseLenkenFraArena)
                .description("Antall brukere under oppfølging som ikke har en bruker data lagret i tabellen: oppfolgingsbruker_arena_v2")
                .register(meterRegistry);
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