package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_CV.*;
import static no.nav.pto.veilarbportefolje.util.DbUtils.boolToJaNei;

@Repository
@RequiredArgsConstructor
public class CvRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsert(AktorId aktoerId, boolean harDeltCv) {
        SqlUtils.upsert(jdbcTemplate, TABLE_NAME)
                .set(AKTOERID, aktoerId.get())
                .set(HAR_DELT_CV, boolToJaNei(harDeltCv))
                .set(SISTE_MELDING_MOTTATT, Timestamp.from(now()))
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();
    }

    public String harDeltCv(AktorId aktoerId) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs.getString(HAR_DELT_CV))
                .column(HAR_DELT_CV)
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();
    }

}
