package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Siste14aVedtakRepository {
    private final JdbcTemplate db;

    public void upsert(Siste14aVedtakDTO siste14aVedtak) {

        String sql = """
                 insert into siste_14a_vedtak(aktor_id, hovedmal, innsatsgruppe, fattet_dato)
                 values (?, ?, ?, ?)
                 on conflict (aktor_id) do update
                     set (aktor_id, hovedmal, innsatsgruppe, fattet_dato) = (excluded.aktor_id,
                                                                             excluded.hovedmal,
                                                                             excluded.innsatsgruppe,
                                                                             excluded.fattet_dato)
                """;

        db.update(
                sql,
                siste14aVedtak.aktorId.get(),
                siste14aVedtak.hovedmal.name(),
                siste14aVedtak.innsatsgruppe.name(),
                Timestamp.from(siste14aVedtak.fattetDato.toInstant())
        );
    }

    @SneakyThrows
    public Optional<Siste14aVedtakDTO> hentSiste14aVedtak(AktorId aktorId) {

        String sql = "select * from siste_14a_vedtak where aktor_id = ?";

        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::siste14aVedtakMapper, aktorId.get()))
        );
    }

    @SneakyThrows
    private Siste14aVedtakDTO siste14aVedtakMapper(ResultSet rs, int rowNum) {
        return new Siste14aVedtakDTO(
                AktorId.of(rs.getString("aktor_id")),
                Siste14aVedtakDTO.Innsatsgruppe.valueOf(rs.getString("innsatsgruppe")),
                Siste14aVedtakDTO.Hovedmal.valueOf(rs.getString("hovedmal")),
                toZonedDateTime(rs.getTimestamp("fattet_dato")));
    }
}
