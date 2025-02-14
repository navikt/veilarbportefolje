package no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Siste14aVedtakRepository {
    private final JdbcTemplate db;

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    private void insert(Siste14aVedtakForBruker siste14AVedtakForBruker) {
        String sql = """
                 insert into siste_14a_vedtak(bruker_id, hovedmal, innsatsgruppe, fattet_dato, fra_arena)
                 values (?, ?, ?, ?, ?)
                """;

        db.update(
                sql,
                siste14AVedtakForBruker.aktorId.get(),
                siste14AVedtakForBruker.hovedmal != null ? siste14AVedtakForBruker.hovedmal.name() : null,
                siste14AVedtakForBruker.innsatsgruppe.name(),
                Timestamp.from(siste14AVedtakForBruker.fattetDato.toInstant()),
                siste14AVedtakForBruker.fraArena
        );
    }

    public void delete(IdenterForBruker identer) {
        db.update("delete from siste_14a_vedtak where bruker_id = any (?::varchar[])", identerParam(identer));
    }

    @Transactional
    public void upsert(Siste14aVedtakForBruker siste14AVedtakForBruker, IdenterForBruker identer) {
        delete(identer);
        insert(siste14AVedtakForBruker);
    }

    @SneakyThrows
    public Optional<Siste14aVedtakForBruker> hentSiste14aVedtak(IdenterForBruker identer) {

        String sql = "select * from siste_14a_vedtak where bruker_id = any (?::varchar[])";

        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::siste14aVedtakRowMapper, identerParam(identer)))
        );
    }

    private static String identerParam(IdenterForBruker identer) {
        return identer.identer().stream().collect(Collectors.joining(",", "{", "}"));
    }

    @SneakyThrows
    private Siste14aVedtakForBruker siste14aVedtakMapper(ResultSet rs) {
        return new Siste14aVedtakForBruker(
                Optional.ofNullable(rs.getString("bruker_id")).map(AktorId::of).orElse(null),
                Innsatsgruppe.valueOf(rs.getString("innsatsgruppe")),
                Optional.ofNullable(rs.getString("hovedmal")).map(Hovedmal::valueOf).orElse(null),
                toZonedDateTime(rs.getTimestamp("fattet_dato")),
                rs.getBoolean("fra_arena"));
    }

    @SneakyThrows
    public Map<AktorId, Siste14aVedtakForBruker> hentSiste14aVedtakForBrukere(Set<AktorId> brukerIder) {
        Map<AktorId, Siste14aVedtakForBruker> result = new HashMap<>();
        String sql = """
                select bi1.ident as oppslag_ident, s.*
                from siste_14a_vedtak s
                inner join bruker_identer bi1 on bi1.ident = any (?::varchar[])
                inner join bruker_identer bi2 on bi2.person = bi1.person
                where s.bruker_id = bi2.ident
                """;

        return dbReadOnly.query(sql,
                ps -> ps.setString(1, listParam(brukerIder.stream().map(AktorId::get).toList())),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        AktorId ident = AktorId.of(rs.getString("oppslag_ident"));
                        result.put(ident, siste14aVedtakMapper(rs));
                    }
                    return result;
                });
    }

    private Siste14aVedtakForBruker siste14aVedtakRowMapper(ResultSet rs, int rowNum) {
        return siste14aVedtakMapper(rs);
    }

    private static String listParam(List<String> identer) {
        return identer.stream().collect(Collectors.joining(",", "{", "}"));
    }
}
