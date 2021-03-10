package no.nav.pto.veilarbportefolje.sistelest;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static no.nav.pto.veilarbportefolje.database.Table.SIST_LEST_AKTIVITETSPLANEN.*;

@Repository
@RequiredArgsConstructor
public class SistLestRepository {
    private final JdbcTemplate jdbcTemplate;

    public void upsert(SistLestKafkaMelding melding) {
        SqlUtils.upsert(jdbcTemplate, TABLE_NAME)
                .set(AKTOERID, melding.getAktorId().get())
                .set(SIST_LEST_AKTIVITETSPLANEN, DateUtils.toTimestamp(melding.harLestTidspunkt))
                .where(
                        WhereClause.equals(AKTOERID, melding.getAktorId().get())
                ).execute();
    }

    public void slettSistLest(AktorId aktorId) {
        SqlUtils.delete(jdbcTemplate, TABLE_NAME)
                .where(WhereClause.equals(AKTOERID, aktorId.get())).execute();
    }
}
