package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.FodselsnummerUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.DISKRESJONSKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.DOED_FRA_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.ENDRET_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.ER_DOED;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.ETTERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.FODSELSNR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.FORMIDLINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.FORNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.HAR_OPPFOLGINGSSAK;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.HOVEDMAALKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.ISERV_FRA_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.KVALIFISERINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.NAV_KONTOR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.RETTIGHETSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.SIKKERHETSTILTAK_TYPE_KODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.SPERRET_ANSATT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toSqlDateOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OppfolginsbrukerRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public int leggTilEllerEndreOppfolgingsbruker(OppfolgingsbrukerEntity oppfolgingsbruker) {
        if (oppfolgingsbruker == null || oppfolgingsbruker.aktoerid() == null) {
            return 0;
        }

        Optional<ZonedDateTime> sistEndretDato = getEndretDato(oppfolgingsbruker.aktoerid());
        if (oppfolgingsbruker.endret_dato() == null || (sistEndretDato.isPresent() && sistEndretDato.get().isAfter(oppfolgingsbruker.endret_dato()))) {
            log.info("Oppdaterer ikke oppfolgingsbruker: {}", oppfolgingsbruker.aktoerid());
            return 0;
        }
        return upsert(oppfolgingsbruker);
    }


    public Optional<OppfolgingsbrukerEntity> getOppfolgingsBruker(AktorId aktorId) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilOppfolgingsbruker, aktorId.get()))
        );
    }


    private Optional<ZonedDateTime> getEndretDato(String aktorId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s=? ", ENDRET_DATO, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilZonedDateTime, aktorId))
        );
    }

    @SneakyThrows
    private ZonedDateTime mapTilZonedDateTime(ResultSet rs, int row) {
        return toZonedDateTime(rs.getTimestamp(ENDRET_DATO));
    }

    @SneakyThrows
    private OppfolgingsbrukerEntity mapTilOppfolgingsbruker(ResultSet rs, int row) {
        if (rs == null || rs.getString(AKTOERID) == null) {
            return null;
        }
        return new OppfolgingsbrukerEntity(rs.getString(AKTOERID), rs.getString(FODSELSNR), rs.getString(FORMIDLINGSGRUPPEKODE),
                toZonedDateTime(rs.getTimestamp(ISERV_FRA_DATO)), rs.getString(ETTERNAVN), rs.getString(FORNAVN),
                rs.getString(NAV_KONTOR), rs.getString(KVALIFISERINGSGRUPPEKODE), rs.getString(RETTIGHETSGRUPPEKODE),
                rs.getString(HOVEDMAALKODE), rs.getString(SIKKERHETSTILTAK_TYPE_KODE), rs.getString(DISKRESJONSKODE),
                rs.getBoolean(HAR_OPPFOLGINGSSAK), rs.getBoolean(SPERRET_ANSATT), rs.getBoolean(ER_DOED),
                toZonedDateTime(rs.getTimestamp(DOED_FRA_DATO)), toZonedDateTime(rs.getTimestamp(ENDRET_DATO)));
    }

    private int upsert(OppfolgingsbrukerEntity oppfolgingsbruker) {
        java.sql.Date fodselsDato = toSqlDateOrNull(FodselsnummerUtils.lagFodselsdato(oppfolgingsbruker.fodselsnr()));
        String kjonn = FodselsnummerUtils.lagKjonn(oppfolgingsbruker.fodselsnr());

        boolean sperretAnsatt = oppfolgingsbruker.sperret_ansatt() != null && oppfolgingsbruker.sperret_ansatt();
        boolean er_doed = oppfolgingsbruker.er_doed() != null && oppfolgingsbruker.er_doed();
        boolean har_oppfolgingssak = oppfolgingsbruker.har_oppfolgingssak() != null && oppfolgingsbruker.har_oppfolgingssak();

        return db.update("""
                        INSERT INTO OPPFOLGINGSBRUKER_ARENA
                        ( AKTOERID, FODSELSNR, FORMIDLINGSGRUPPEKODE
                        , ISERV_FRA_DATO, ETTERNAVN, FORNAVN
                        , NAV_KONTOR, KVALIFISERINGSGRUPPEKODE, RETTIGHETSGRUPPEKODE
                        , HOVEDMAALKODE, SIKKERHETSTILTAK_TYPE_KODE, DISKRESJONSKODE
                        , HAR_OPPFOLGINGSSAK, SPERRET_ANSATT, ER_DOED
                        , DOED_FRA_DATO, ENDRET_DATO , KJONN
                        , FODSELS_DATO)
                        VALUES(?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?)
                        ON CONFLICT (AKTOERID) DO UPDATE SET
                        (FODSELSNR, FORMIDLINGSGRUPPEKODE, ISERV_FRA_DATO
                        , ETTERNAVN, FORNAVN, NAV_KONTOR
                        , KVALIFISERINGSGRUPPEKODE, RETTIGHETSGRUPPEKODE, HOVEDMAALKODE
                        , SIKKERHETSTILTAK_TYPE_KODE, DISKRESJONSKODE, HAR_OPPFOLGINGSSAK
                        , SPERRET_ANSATT, ER_DOED, DOED_FRA_DATO
                        , ENDRET_DATO, KJONN, FODSELS_DATO
                        ) = (?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?)
                        """,
                oppfolgingsbruker.aktoerid(), oppfolgingsbruker.fodselsnr(), oppfolgingsbruker.formidlingsgruppekode(),
                toTimestamp(oppfolgingsbruker.iserv_fra_dato()), oppfolgingsbruker.etternavn(), oppfolgingsbruker.fornavn(),
                oppfolgingsbruker.nav_kontor(), oppfolgingsbruker.kvalifiseringsgruppekode(), oppfolgingsbruker.rettighetsgruppekode(),
                oppfolgingsbruker.hovedmaalkode(), oppfolgingsbruker.sikkerhetstiltak_type_kode(), oppfolgingsbruker.fr_kode(),
                har_oppfolgingssak, sperretAnsatt, er_doed,
                toTimestamp(oppfolgingsbruker.doed_fra_dato()), toTimestamp(oppfolgingsbruker.endret_dato()), kjonn,
                fodselsDato,

                oppfolgingsbruker.fodselsnr(), oppfolgingsbruker.formidlingsgruppekode(), toTimestamp(oppfolgingsbruker.iserv_fra_dato()),
                oppfolgingsbruker.etternavn(), oppfolgingsbruker.fornavn(), oppfolgingsbruker.nav_kontor(),
                oppfolgingsbruker.kvalifiseringsgruppekode(), oppfolgingsbruker.rettighetsgruppekode(), oppfolgingsbruker.hovedmaalkode(),
                oppfolgingsbruker.sikkerhetstiltak_type_kode(), oppfolgingsbruker.fr_kode(), har_oppfolgingssak,
                sperretAnsatt, er_doed, toTimestamp(oppfolgingsbruker.doed_fra_dato()),
                toTimestamp(oppfolgingsbruker.endret_dato()), kjonn, fodselsDato
        );
    }
}
