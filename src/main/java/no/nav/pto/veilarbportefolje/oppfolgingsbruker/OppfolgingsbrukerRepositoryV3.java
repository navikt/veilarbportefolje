package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
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
    private final DefaultUnleash defaultUnleash;

    @Transactional
    public int leggTilEllerEndreOppfolgingsbruker(OppfolgingsbrukerEntity oppfolgingsbruker, NavKontor navKontor, AktorId aktorId) {
        if (oppfolgingsbruker == null || oppfolgingsbruker.fodselsnr() == null) {
            return 0;
        }

        Optional<ZonedDateTime> sistEndretDato = getEndretDatoForUpdate(Fnr.of(oppfolgingsbruker.fodselsnr()));
        if (oppfolgingsbruker.endret_dato() == null || (sistEndretDato.isPresent() && sistEndretDato.get().isAfter(oppfolgingsbruker.endret_dato()))) {
            return 0;
        }

        var rowsChanged = upsert(oppfolgingsbruker);
        if (navKontor != null) upsertNavKontor(aktorId, Fnr.of(oppfolgingsbruker.fodselsnr()), navKontor);
        return rowsChanged;
    }

    public int slettOppfolgingsbruker(Fnr fnr) {
        return db.update(
                String.format("DELETE FROM %s WHERE fodselsnr = ?", PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.TABLE_NAME),
                fnr.get()
        );
    }


    private Optional<ZonedDateTime> getEndretDatoForUpdate(Fnr fnr) {
        String sql = "SELECT endret_dato FROM oppfolgingsbruker_arena_v2 WHERE fodselsnr = ? for update";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilZonedDateTime, fnr.get()))
        );
    }

    public void upsertNavKontor(AktorId aktorId, Fnr fnr, NavKontor navKontor) {
        var params = new MapSqlParameterSource()
                .addValue("ident", fnr.get())
                .addValue("navKontor", navKontor.getValue())
                .addValue("aktorId", aktorId.get());
        dbNamed.update("""
                    INSERT INTO ao_kontor (ident, kontor_id, aktorid) VALUES (:ident, :navKontor, :aktorId)
                    ON CONFLICT (aktorid) DO UPDATE SET ident = EXCLUDED.ident, kontor_id = EXCLUDED.kontor_id, updated_at = CURRENT_TIMESTAMP
                """, params);
    }

    public void slettNavKontor(AktorId aktorId) {
        var params = new MapSqlParameterSource()
                .addValue("aktorId", aktorId.get());
        dbNamed.update("""
                    DELETE FROM ao_kontor WHERE aktorid = :aktorId
                """, params);
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
        boolean brukAoKontor = FeatureToggle.brukKontorFraAoKontor(defaultUnleash);
        var sql = """
                SELECT coalesce(CASE WHEN :brukAoKontor::boolean THEN ao.kontor_id ELSE NULL END, ob.nav_kontor) AS kontor_id
                FROM oppfolgingsbruker_arena_v2 ob
                LEFT JOIN ao_kontor ao ON ob.fodselsnr = ao.ident
                WHERE ob.fodselsnr = :ident
                """;
        var params = new MapSqlParameterSource()
                .addValue("ident", fnr.get())
                .addValue("brukAoKontor", brukAoKontor);
        return Optional.ofNullable(
                queryForObjectOrNull(
                        () -> dbNamed.queryForObject(sql, params, (rs, i) ->
                                NavKontor.navKontorOrNull(rs.getString("kontor_id")))
                ));
    }
}
