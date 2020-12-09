package no.nav.pto.veilarbportefolje.sisteendring;

import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
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
                .set(AKTOERID, sisteEndringDTO.getAktoerId().toString())
                .set(SISTE_ENDRING_KATEGORI, sisteEndringDTO.getKategori().toString())
                .set(SISTE_ENDRING_TIDSPUNKT, Timestamp.from(sisteEndringDTO.getTidspunkt().toInstant()))
                .where(WhereClause.equals(AKTOERID, sisteEndringDTO.getAktoerId().toString()))
                .execute();
    }

    public Timestamp getSisteEndringTidspunkt(AktoerId aktoerId, SisteEndringsKategorier kategori) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT))
                .column(SISTE_ENDRING_TIDSPUNKT)
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()).and(
                        WhereClause.equals(SISTE_ENDRING_KATEGORI, kategori.toString())
                ))
                .execute();
    }
}


