package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;


import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.ENDRET_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.FODSELSNR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.FORMIDLINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.HOVEDMAALKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.ISERV_FRA_DATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.KVALIFISERINGSGRUPPEKODE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.NAV_KONTOR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.RETTIGHETSGRUPPEKODE;
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

    public int slettOppfolgingsbruker(Fnr fnr) {
        return db.update("DELETE FROM oppfolgingsbruker_arena_v2 WHERE fodselsnr = ?", fnr.get());
    }

    private record OppfolgingsbrukerEntityMedOppslagFnr(
            Fnr oppslagFnr,
            OppfolgingsbrukerEntity oppfolgingsbrukerEntity
    ) {
    }

    public Map<Fnr, OppfolgingsbrukerEntity> hentOppfolgingsBrukere(Set<Fnr> fnrs) {
        String fnrsCondition = fnrs.stream().map(Fnr::toString).collect(Collectors.joining(",", "{", "}"));
        String sql = """
                                SELECT DISTINCT ON (bi1.ident)
                                     bi1.ident as oppslag_fnr, ob.*
                                FROM OPPFOLGINGSBRUKER_ARENA_V2 ob
                                INNER JOIN BRUKER_IDENTER bi1 on bi1.ident = any (?::varchar[])
                                INNER JOIN BRUKER_IDENTER bi2 on bi2.person = bi1.person
                                WHERE ob.fodselsnr = bi2.ident
                                AND bi2.gruppe = ?
                """;

        return db.query(sql,
                        OppfolgingsbrukerRepositoryV3::mapTilOppfolgingsbrukerMedOppslagFnr,
                        fnrsCondition,
                        PDLIdent.Gruppe.FOLKEREGISTERIDENT.name())
                .stream().filter(Objects::nonNull).collect(Collectors.toMap(
                        OppfolgingsbrukerEntityMedOppslagFnr::oppslagFnr,
                        OppfolgingsbrukerEntityMedOppslagFnr::oppfolgingsbrukerEntity)
                );
    }

    @SneakyThrows
    private static OppfolgingsbrukerEntityMedOppslagFnr mapTilOppfolgingsbrukerMedOppslagFnr(ResultSet rs, int row) {
        OppfolgingsbrukerEntity oppfolgingsbrukerEntity = mapTilOppfolgingsbruker(rs, row);

        return oppfolgingsbrukerEntity != null
                ? new OppfolgingsbrukerEntityMedOppslagFnr(Fnr.of(rs.getString("oppslag_fnr")), oppfolgingsbrukerEntity)
                : null;
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
                        nav_kontor,
                        kvalifiseringsgruppekode, rettighetsgruppekode,
                        hovedmaalkode,
                        endret_dato)
                        VALUES(?,?,?,?,?,?,?,?)
                        ON CONFLICT (fodselsnr) DO UPDATE SET(
                        formidlingsgruppekode, iserv_fra_dato,
                        nav_kontor,
                        kvalifiseringsgruppekode, rettighetsgruppekode,
                        hovedmaalkode,
                        endret_dato)
                        = (excluded.formidlingsgruppekode, excluded.iserv_fra_dato,
                        excluded.nav_kontor,
                        excluded.kvalifiseringsgruppekode, excluded.rettighetsgruppekode,
                        excluded.hovedmaalkode,
                        excluded.endret_dato)
                        """,
                oppfolgingsbruker.fodselsnr(), oppfolgingsbruker.formidlingsgruppekode(), toTimestamp(oppfolgingsbruker.iserv_fra_dato()),
                oppfolgingsbruker.nav_kontor(),
                oppfolgingsbruker.kvalifiseringsgruppekode(), oppfolgingsbruker.rettighetsgruppekode(),
                oppfolgingsbruker.hovedmaalkode(),
                toTimestamp(oppfolgingsbruker.endret_dato())
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
                toZonedDateTime(rs.getTimestamp(ISERV_FRA_DATO)),
                rs.getString(NAV_KONTOR), rs.getString(KVALIFISERINGSGRUPPEKODE), rs.getString(RETTIGHETSGRUPPEKODE),
                rs.getString(HOVEDMAALKODE),
                toZonedDateTime(rs.getTimestamp(ENDRET_DATO)));
    }

    public List<String> finnSkjulteBrukere(List<String> fnrListe, BrukerinnsynTilganger brukerInnsynTilganger) {
        var params = new MapSqlParameterSource();
        params.addValue("fnrListe", fnrListe.stream().collect(Collectors.joining(",", "{", "}")));
        params.addValue("tilgangTilEgenAnsatt", brukerInnsynTilganger.tilgangTilSkjerming());
        params.addValue("tilgangTilAdressebeskyttelseStrengtFortrolig", brukerInnsynTilganger.tilgangTilAdressebeskyttelseStrengtFortrolig());
        params.addValue("tilgangTilAdressebeskyttelseFortrolig", brukerInnsynTilganger.tilgangTilAdressebeskyttelseFortrolig());

        return dbNamed.queryForList("""
                SELECT oa.fodselsnr from oppfolgingsbruker_arena_v2 oa
                left join nom_skjerming ns on  ns.fodselsnr = oa.fodselsnr
                left join bruker_data bd on  bd.freg_ident = oa.fodselsnr
                where oa.fodselsnr = ANY (:fnrListe::varchar[])
                AND (
                                    (bd.diskresjonkode = '6' AND NOT :tilgangTilAdressebeskyttelseStrengtFortrolig::boolean)
                                    OR (bd.diskresjonkode = '7' AND NOT :tilgangTilAdressebeskyttelseFortrolig::boolean)
                                    OR (ns.er_skjermet AND NOT :tilgangTilEgenAnsatt::boolean)
                )
                """, params, String.class);
    }

    public Optional<NavKontor> hentNavKontor(Fnr fnr) {
        return Optional.ofNullable(
                queryForObjectOrNull(
                        () -> db.queryForObject("select nav_kontor from oppfolgingsbruker_arena_v2 where fodselsnr = ?",
                                (rs, i) -> NavKontor.navKontorOrNull(rs.getString("nav_kontor")), fnr.get())
                ));
    }
}
