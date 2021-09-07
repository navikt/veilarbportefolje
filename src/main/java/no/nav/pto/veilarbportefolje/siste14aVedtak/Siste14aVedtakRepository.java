package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.database.Table.SISTE_14A_VEDTAK;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZoneId;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Siste14aVedtakRepository {
    private final JdbcTemplate db;

    public void upsert(Siste14aVedtakDTO siste14aVedtak) {
        SqlUtils.upsert(db, SISTE_14A_VEDTAK.TABLE_NAME)
                .set(SISTE_14A_VEDTAK.AKTOERID, siste14aVedtak.aktorId.get())
                .set(SISTE_14A_VEDTAK.HOVEDMAL, siste14aVedtak.hovedmal.toString())
                .set(SISTE_14A_VEDTAK.INNSATSGRUPPE, siste14aVedtak.innsatsgruppe.toString())
                .set(SISTE_14A_VEDTAK.FATTET_DATO, Timestamp.valueOf(siste14aVedtak.fattetDato.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()))
                .where(WhereClause.equals(Table.OPPFOLGING_DATA.AKTOERID, siste14aVedtak.aktorId.get()))
                .execute();
    }

    @SneakyThrows
    public Siste14aVedtakDTO hentSiste14aVedtak(AktorId aktoerId) {
        return SqlUtils
                .select(db, SISTE_14A_VEDTAK.TABLE_NAME, this::siste14aVedtakMapper)
                .column("*")
                .where(WhereClause.equals(SISTE_14A_VEDTAK.AKTOERID, aktoerId.get())).execute();
    }

    @SneakyThrows
    private Siste14aVedtakDTO siste14aVedtakMapper(ResultSet rs) {
        return new Siste14aVedtakDTO(
                AktorId.of(rs.getString(SISTE_14A_VEDTAK.AKTOERID)),
                Siste14aVedtakDTO.Innsatsgruppe.valueOf(rs.getString(SISTE_14A_VEDTAK.INNSATSGRUPPE)),
                Siste14aVedtakDTO.Hovedmal.valueOf(rs.getString(SISTE_14A_VEDTAK.HOVEDMAL)),
                toZonedDateTime(rs.getTimestamp(SISTE_14A_VEDTAK.FATTET_DATO)));
    }
}
