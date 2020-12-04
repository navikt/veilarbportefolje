package no.nav.pto.veilarbportefolje.sisteendring;

import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import static no.nav.pto.veilarbportefolje.database.Table.SISTE_ENDRING.*;

@Repository
public class SisteEndringRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SisteEndringRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(SisteEndringDTO sisteEndringDTO) {
        SqlUtils.upsert(jdbcTemplate, TABLE_NAME)
                .set(AKTOERID, sisteEndringDTO.aktoerId.toString())
                .set(SISTE_ENDRING_KATEGORI, sisteEndringDTO.kategori.toString())
                .set(SISTE_ENDRING_TIDSPUNKT, sisteEndringDTO.tidspunkt)
                .where(WhereClause.equals(AKTOERID, sisteEndringDTO.aktoerId.toString()))
                .execute();
    }

    public String getSisteEndringKategori(AktoerId aktoerId) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs.getString(SISTE_ENDRING_KATEGORI))
                .column(SISTE_ENDRING_KATEGORI)
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();
    }

    public Timestamp getSisteEndringTidspunkt(AktoerId aktoerId) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT))
                .column(SISTE_ENDRING_TIDSPUNKT)
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();
    }
}


