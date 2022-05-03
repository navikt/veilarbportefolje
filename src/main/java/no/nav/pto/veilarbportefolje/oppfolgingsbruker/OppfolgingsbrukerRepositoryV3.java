package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.Skjermettilgang;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.sbl.sql.SqlUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static no.nav.common.utils.CollectionUtils.partition;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.DISKRESJONSKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.ENDRET_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.ER_DOED;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.ETTERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.FODSELSNR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.FORMIDLINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.FORNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.HOVEDMAALKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.ISERV_FRA_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.KVALIFISERINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.NAV_KONTOR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.RETTIGHETSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.SIKKERHETSTILTAK_TYPE_KODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.SPERRET_ANSATT;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parseJaNei;
import static no.nav.sbl.sql.where.WhereClause.in;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OppfolgingsbrukerRepositoryV3 {
    private final JdbcTemplate oracle_db;
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate dbNamed;

    public int leggTilEllerEndreOppfolgingsbruker(OppfolgingsbrukerEntity oppfolgingsbruker) {
        if (oppfolgingsbruker == null || oppfolgingsbruker.fodselsnr() == null) {
            return 0;
        }

        Optional<ZonedDateTime> sistEndretDato = getEndretDato(Fnr.of(oppfolgingsbruker.fodselsnr()));
        if (oppfolgingsbruker.endret_dato() == null || (sistEndretDato.isPresent() && sistEndretDato.get().isAfter(oppfolgingsbruker.endret_dato()))) {
            return 0;
        }
        return upsert(oppfolgingsbruker);
    }

    public Optional<OppfolgingsbrukerEntity> getOppfolgingsBruker(Fnr fnr) {
        String sql = "SELECT * FROM OPPFOLGINGSBRUKER_ARENA_V2 WHERE fodselsnr = ?";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilOppfolgingsbruker, fnr.get()))
        );
    }


    private Optional<ZonedDateTime> getEndretDato(Fnr fnr) {
        String sql = "SELECT endret_dato FROM oppfolgingsbruker_arena_v2 WHERE fodselsnr = ?";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilZonedDateTime, fnr.get()))
        );
    }

    private int upsert(OppfolgingsbrukerEntity oppfolgingsbruker) {
        return db.update("""
                        INSERT INTO oppfolgingsbruker_arena_v2(
                        fodselsnr, formidlingsgruppekode, iserv_fra_dato,
                        etternavn, fornavn, nav_kontor,
                        kvalifiseringsgruppekode, rettighetsgruppekode,
                        hovedmaalkode, sikkerhetstiltak_type_kode, diskresjonskode,
                        sperret_ansatt, er_doed, endret_dato)
                        VALUES(?,?,?, ?,?,?, ?,?, ?,?,?, ?,?,?)
                        ON CONFLICT (fodselsnr) DO UPDATE SET(
                        formidlingsgruppekode, iserv_fra_dato,
                        etternavn, fornavn, nav_kontor,
                        kvalifiseringsgruppekode, rettighetsgruppekode,
                        hovedmaalkode, sikkerhetstiltak_type_kode, diskresjonskode,
                        sperret_ansatt, er_doed, endret_dato)
                        = (excluded.formidlingsgruppekode, excluded.iserv_fra_dato,
                        excluded.etternavn, excluded.fornavn, excluded.nav_kontor,
                        excluded.kvalifiseringsgruppekode, excluded.rettighetsgruppekode,
                        excluded.hovedmaalkode, excluded.sikkerhetstiltak_type_kode, excluded.diskresjonskode,
                        excluded.sperret_ansatt, excluded.er_doed, excluded.endret_dato)
                        """,
                oppfolgingsbruker.fodselsnr(), oppfolgingsbruker.formidlingsgruppekode(), toTimestamp(oppfolgingsbruker.iserv_fra_dato()),
                oppfolgingsbruker.etternavn(), oppfolgingsbruker.fornavn(), oppfolgingsbruker.nav_kontor(),
                oppfolgingsbruker.kvalifiseringsgruppekode(), oppfolgingsbruker.rettighetsgruppekode(),
                oppfolgingsbruker.hovedmaalkode(), oppfolgingsbruker.sikkerhetstiltak_type_kode(), oppfolgingsbruker.fr_kode(),
                oppfolgingsbruker.sperret_ansatt(), oppfolgingsbruker.er_doed(), toTimestamp(oppfolgingsbruker.endret_dato())
        );
    }

    @SneakyThrows
    private ZonedDateTime mapTilZonedDateTime(ResultSet rs, int row) {
        return toZonedDateTime(rs.getTimestamp(ENDRET_DATO));
    }

    @SneakyThrows
    private OppfolgingsbrukerEntity mapTilOppfolgingsbruker(ResultSet rs, int row) {
        if (rs == null || rs.getString(FODSELSNR) == null) {
            return null;
        }
        return new OppfolgingsbrukerEntity(null, rs.getString(FODSELSNR), rs.getString(FORMIDLINGSGRUPPEKODE),
                toZonedDateTime(rs.getTimestamp(ISERV_FRA_DATO)), rs.getString(ETTERNAVN), rs.getString(FORNAVN),
                rs.getString(NAV_KONTOR), rs.getString(KVALIFISERINGSGRUPPEKODE), rs.getString(RETTIGHETSGRUPPEKODE),
                rs.getString(HOVEDMAALKODE), rs.getString(SIKKERHETSTILTAK_TYPE_KODE), rs.getString(DISKRESJONSKODE),
                false, rs.getBoolean(SPERRET_ANSATT), rs.getBoolean(ER_DOED),
                null, toZonedDateTime(rs.getTimestamp(ENDRET_DATO)));
    }

    public List<String> finnSkjulteBrukere(List<String> fnrListe, Skjermettilgang skjermettilgang) {
        var params = new MapSqlParameterSource();
        params.addValue("fnrListe", fnrListe.stream().collect(Collectors.joining(",", "{", "}")));
        params.addValue("tilgangTilKode6", skjermettilgang.tilgangTilKode6());
        params.addValue("tilgangTilKode7", skjermettilgang.tilgangTilKode7());
        params.addValue("tilgangTilEgenAnsatt", skjermettilgang.tilgangTilEgenAnsatt());

        return dbNamed.queryForList("""
                SELECT fodselsnr from oppfolgingsbruker_arena_v2
                where fodselsnr = ANY (:fnrListe::varchar[])
                AND (
                    (diskresjonskode = '6' AND NOT :tilgangTilKode6::boolean)
                    OR (diskresjonskode = '7' AND NOT :tilgangTilKode7::boolean)
                    OR (sperret_ansatt AND NOT :tilgangTilEgenAnsatt::boolean)
                )""", params, String.class);
    }

    public String migrerOppfolgingsbrukere() {
        AtomicInteger antallFeilet = new AtomicInteger();
        var brukereITabell = oracle_db.queryForList("SELECT FODSELSNR FROM OPPFOLGINGSBRUKER", String.class);
        partition(brukereITabell, 10_000).forEach(bolk -> {
            List<OppfolgingsbrukerEntity> oppfolgingsbrukerEntities = SqlUtils.select(db, Table.VW_PORTEFOLJE_INFO.TABLE_NAME, this::entityFromOracle)
                    .column("*")
                    .where(in("FODSELSNR", brukereITabell))
                    .executeToList()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();

            for (var entity : oppfolgingsbrukerEntities) {
                try {
                    leggTilEllerEndreOppfolgingsbruker(entity);
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
                rs.getString("HOVEDMAALKODE"), rs.getString("SIKKERHETSTILTAK_TYPE_KODE"), rs.getString("DISKRESJONSKODE"),
                false, parseJaNei(rs.getString("SPERRET_ANSATT"), "SPERRET_ANSATT"), parseJaNei(rs.getString("ER_DOED"), "ER_DOED"),
                null, toZonedDateTime(rs.getTimestamp("ENDRET_DATO")));
    }
}
