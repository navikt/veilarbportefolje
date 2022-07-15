package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
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

    private void insert(Siste14aVedtak siste14aVedtak) {
        String sql = """
                 insert into siste_14a_vedtak(bruker_id, hovedmal, innsatsgruppe, fattet_dato, fra_arena)
                 values (?, ?, ?, ?, ?)
                """;

        db.update(
                sql,
                siste14aVedtak.brukerId,
                siste14aVedtak.hovedmal != null ? siste14aVedtak.hovedmal.name() : null,
                siste14aVedtak.innsatsgruppe.name(),
                Timestamp.from(siste14aVedtak.fattetDato.toInstant()),
                siste14aVedtak.fraArena
        );
    }

    public void delete(IdenterForBruker identer) {
        db.update("delete from siste_14a_vedtak where bruker_id = any (?::varchar[])", listParam(identer.identer()));
    }

    @Transactional
    public void upsert(Siste14aVedtak siste14aVedtak, IdenterForBruker identer) {
        delete(identer);
        insert(siste14aVedtak);
    }

    @SneakyThrows
    public Optional<Siste14aVedtak> hentSiste14aVedtak(IdenterForBruker identer) {

        String sql = "select * from siste_14a_vedtak where bruker_id = any (?::varchar[])";

        return Optional.ofNullable(
                queryForObjectOrNull(() ->
                        dbReadOnly.queryForObject(sql, this::siste14aVedtakRowMapper, listParam(identer.identer()))
                )
        );
    }

    @SneakyThrows
    public Map<String, Siste14aVedtak> hentSiste14aVedtakForBrukere(Set<String> brukerIder) {
        Map<String, Siste14aVedtak> result = new HashMap<>();
        String sql = """
                select bi1.ident as oppslag_ident, s.*
                from siste_14a_vedtak s
                inner join bruker_identer bi1 on bi1.ident = any (?::varchar[])
                inner join bruker_identer bi2 on bi2.person = bi1.person
                where s.bruker_id = bi2.ident
                """;

        return dbReadOnly.query(sql,
                ps -> ps.setString(1, listParam(brukerIder.stream().toList())),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        String ident = rs.getString("oppslag_ident");
                        result.put(ident, siste14aVedtakMapper(rs));
                    }
                    return result;
                });
    }

    private static String listParam(List<String> identer) {
        return identer.stream().collect(Collectors.joining(",", "{", "}"));
    }

    private Siste14aVedtak siste14aVedtakRowMapper(ResultSet rs, int rowNum) {
        return siste14aVedtakMapper(rs);
    }
    @SneakyThrows

    private Siste14aVedtak siste14aVedtakMapper(ResultSet rs) {
        return new Siste14aVedtak(
                rs.getString("bruker_id"),
                Siste14aVedtakKafkaDTO.Innsatsgruppe.valueOf(rs.getString("innsatsgruppe")),
                Optional.ofNullable(rs.getString("hovedmal")).map(Siste14aVedtakKafkaDTO.Hovedmal::valueOf).orElse(null),
                toZonedDateTime(rs.getTimestamp("fattet_dato")),
                rs.getBoolean("fra_arena"));
    }
}
