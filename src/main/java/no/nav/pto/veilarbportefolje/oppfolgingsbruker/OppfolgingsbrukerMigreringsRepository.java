package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.sbl.sql.SqlUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parseJaNei;
import static no.nav.sbl.sql.where.WhereClause.in;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OppfolgingsbrukerMigreringsRepository {
    private final JdbcTemplate oracle_db;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;

    public String migrerOppfolgingsbrukere() {
        AtomicInteger antallFeilet = new AtomicInteger();
        var brukereITabell = oracle_db.queryForList("SELECT FODSELSNR FROM OPPFOLGINGSBRUKER", String.class);
        partition(brukereITabell, 1000).forEach(bolk -> {
            List<OppfolgingsbrukerEntity> oppfolgingsbrukerEntities = SqlUtils.select(oracle_db, Table.VW_PORTEFOLJE_INFO.TABLE_NAME, this::entityFromOracle)
                    .column("*")
                    .where(in("FODSELSNR", bolk))
                    .executeToList()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();

            for (var entity : oppfolgingsbrukerEntities) {
                try {
                    oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(entity);
                } catch (Exception e) {
                    antallFeilet.addAndGet(1);
                }
            }
            log.info("ferdig med en bolk");
        });

        log.info("Migrering ferdig. Antall som feilet: {}", antallFeilet.get());
        return "Antall migreringer som feilet: " + antallFeilet.get();
    }


    @SneakyThrows
    private OppfolgingsbrukerEntity entityFromOracle(ResultSet rs) {
        return new OppfolgingsbrukerEntity(null, rs.getString("FODSELSNR"), rs.getString("FORMIDLINGSGRUPPEKODE"),
                toZonedDateTime(rs.getTimestamp("ISERV_FRA_DATO")), rs.getString("ETTERNAVN"), rs.getString("FORNAVN"),
                rs.getString("NAV_KONTOR"), rs.getString("KVALIFISERINGSGRUPPEKODE"), rs.getString("RETTIGHETSGRUPPEKODE"),
                rs.getString("HOVEDMAALKODE"), rs.getString("SIKKERHETSTILTAK_TYPE_KODE"), rs.getString("FR_KODE"),
                false, parseJaNei(rs.getString("SPERRET_ANSATT"), "SPERRET_ANSATT"), parseJaNei(rs.getString("ER_DOED"), "ER_DOED"),
                null, toZonedDateTime(rs.getTimestamp("TIDSSTEMPEL")));
    }
}
