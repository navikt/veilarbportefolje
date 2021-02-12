package no.nav.pto.veilarbportefolje.pdldata;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import static no.nav.pto.veilarbportefolje.database.Table.PDL_DATA.*;
import static no.nav.pto.veilarbportefolje.database.Table.SISTE_ENDRING.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.Table.SISTE_ENDRING.TABLE_NAME;

@Slf4j
@Repository
public class PdlRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PdlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(AktorId aktorId, Timestamp fodseldag) {
        SqlUtils.upsert(jdbcTemplate, TABLE_NAME)
                .set(AKTOERID, aktorId.get())
                .set(FODSELSDAG, fodseldag)
                .where(WhereClause.equals(AKTOERID, aktorId.get()))
                .execute();
    }

    public void slettPdlData(AktorId aktorId) {
        SqlUtils.delete(jdbcTemplate, TABLE_NAME)
                .where(WhereClause.equals(AKTOERID, aktorId.get())).execute();
    }
}
