package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.Skjermettilgang;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.DISKRESJONSKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.ENDRET_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.ER_DOED;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.ETTERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.FODSELSNR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.FORMIDLINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.FORNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.HOVEDMAALKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.ISERV_FRA_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.KVALIFISERINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.NAV_KONTOR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.RETTIGHETSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.SIKKERHETSTILTAK_TYPE_KODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.SPERRET_ANSATT;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OppfolgingsbrukerRepositoryV3 {
    private final JdbcTemplate db;
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate dbNamed;

    @Transactional
    public int leggTilEllerEndreOppfolgingsbruker(OppfolgingsbrukerEntity oppfolgingsbruker) {
        if (oppfolgingsbruker == null || oppfolgingsbruker.fodselsnr() == null) {
            return 0;
        }

        Optional<ZonedDateTime> sistEndretDato = getEndretDatoForUpdate(Fnr.of(oppfolgingsbruker.fodselsnr()));
        if (oppfolgingsbruker.endret_dato() == null || (sistEndretDato.isPresent() && sistEndretDato.get().isAfter(oppfolgingsbruker.endret_dato()))) {
            return 0;
        }
        return upsert(oppfolgingsbruker);
    }

    public Map<Fnr, OppfolgingsbrukerEntity> hentOppfolgingsBrukere(Set<Fnr> fnrs) {
        String fnrsCondition = fnrs.stream().map(Fnr::toString).collect(Collectors.joining(",", "{", "}"));
        String sql = "SELECT * FROM OPPFOLGINGSBRUKER_ARENA_V2 WHERE fodselsnr = any (?::varchar[])";
        return db.query(sql, OppfolgingsbrukerRepositoryV3::mapTilOppfolgingsbruker, fnrsCondition)
                .stream().collect(Collectors.toMap(oppfolgingsbruker -> Fnr.of(oppfolgingsbruker.fodselsnr()), Function.identity()));
    }

    public Optional<OppfolgingsbrukerEntity> getOppfolgingsBruker(Fnr fnr) {
        String sql = "SELECT * FROM OPPFOLGINGSBRUKER_ARENA_V2 WHERE fodselsnr = ?";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, OppfolgingsbrukerRepositoryV3::mapTilOppfolgingsbruker, fnr.get()))
        );
    }


    private Optional<ZonedDateTime> getEndretDatoForUpdate(Fnr fnr) {
        String sql = "SELECT endret_dato FROM oppfolgingsbruker_arena_v2 WHERE fodselsnr = ? for update";
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
    public static OppfolgingsbrukerEntity mapTilOppfolgingsbruker(ResultSet rs, int row) {
        if (rs == null || rs.getString(FODSELSNR) == null) {
            return null;
        }
        return new OppfolgingsbrukerEntity(rs.getString(FODSELSNR), rs.getString(FORMIDLINGSGRUPPEKODE),
                toZonedDateTime(rs.getTimestamp(ISERV_FRA_DATO)), rs.getString(ETTERNAVN), rs.getString(FORNAVN),
                rs.getString(NAV_KONTOR), rs.getString(KVALIFISERINGSGRUPPEKODE), rs.getString(RETTIGHETSGRUPPEKODE),
                rs.getString(HOVEDMAALKODE), rs.getString(SIKKERHETSTILTAK_TYPE_KODE), rs.getString(DISKRESJONSKODE),
                rs.getBoolean(SPERRET_ANSATT), rs.getBoolean(ER_DOED), toZonedDateTime(rs.getTimestamp(ENDRET_DATO)));
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

    public Optional<NavKontor> hentNavKontor(Fnr fnr) {
        return Optional.ofNullable(
                queryForObjectOrNull(
                        () -> db.queryForObject("select nav_kontor from oppfolgingsbruker_arena_v2 where fodselsnr = ?",
                                (rs, i) -> NavKontor.navKontorOrNull(rs.getString("nav_kontor")), fnr.get())
                ));
    }
}
