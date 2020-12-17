package no.nav.pto.veilarbportefolje.cv;

import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_CV.*;
import static no.nav.pto.veilarbportefolje.util.DbUtils.boolToJaNei;

@Repository
public class CvRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CvRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(AktoerId aktoerId, boolean harDeltCv) {
        SqlUtils.upsert(jdbcTemplate, TABLE_NAME)
                .set(AKTOERID, aktoerId.toString())
                .set(HAR_DELT_CV, boolToJaNei(harDeltCv))
                .set(SISTE_MELDING_MOTTATT, Timestamp.from(now()))
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();
    }

    public String harDeltCv(AktoerId aktoerId) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs.getString(HAR_DELT_CV))
                .column(HAR_DELT_CV)
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();
    }

}
