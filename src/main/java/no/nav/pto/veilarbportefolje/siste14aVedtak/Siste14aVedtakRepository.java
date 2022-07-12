package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Siste14aVedtakRepository {
    private final JdbcTemplate db;

    private void insert(Siste14aVedtak siste14aVedtak) {
        String sql = """
                 insert into siste_14a_vedtak(bruker_id, hovedmal, innsatsgruppe, fattet_dato, fra_arena)
                 values (?, ?, ?, ?, ?)
                """;

        db.update(
                sql,
                siste14aVedtak.brukerId,
                siste14aVedtak.hovedmal.name(),
                siste14aVedtak.innsatsgruppe.name(),
                Timestamp.from(siste14aVedtak.fattetDato.toInstant()),
                siste14aVedtak.fraArena
        );
    }

    public void delete(IdenterForBruker identer) {
        db.update("delete from siste_14a_vedtak where bruker_id = any (?::varchar[])", identerParam(identer));
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
                queryForObjectOrNull(() -> db.queryForObject(sql, this::siste14aVedtakMapper, identerParam(identer)))
        );
    }

    private static String identerParam(IdenterForBruker identer) {
        return identer.identer().stream().collect(Collectors.joining(",", "{", "}"));
    }

    @SneakyThrows
    private Siste14aVedtak siste14aVedtakMapper(ResultSet rs, int rowNum) {
        return new Siste14aVedtak(
                rs.getString("bruker_id"),
                Siste14aVedtakKafkaDTO.Innsatsgruppe.valueOf(rs.getString("innsatsgruppe")),
                Siste14aVedtakKafkaDTO.Hovedmal.valueOf(rs.getString("hovedmal")),
                toZonedDateTime(rs.getTimestamp("fattet_dato")),
                rs.getBoolean("fra_arena"));
    }
}
