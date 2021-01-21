package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.domene.Endring;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.database.Table.SISTE_ENDRING.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;

@Slf4j
@Repository
public class SisteEndringRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SisteEndringRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(SisteEndringDTO sisteEndringDTO) {
        SqlUtils.upsert(jdbcTemplate, TABLE_NAME)
                .set(AKTOERID, sisteEndringDTO.getAktoerId().getValue())
                .set(SISTE_ENDRING_KATEGORI, sisteEndringDTO.getKategori().name())
                .set(SISTE_ENDRING_TIDSPUNKT, Timestamp.from(sisteEndringDTO.getTidspunkt().toInstant()))
                .set(AKTIVITETID, sisteEndringDTO.getAktivtetId())
                .where(WhereClause.equals(AKTOERID, sisteEndringDTO.getAktoerId().getValue()).and(
                        WhereClause.equals(SISTE_ENDRING_KATEGORI, sisteEndringDTO.getKategori().name())
                )).execute();
    }

    public Timestamp getSisteEndringTidspunkt(AktoerId aktoerId, SisteEndringsKategori kategori) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT))
                .column(SISTE_ENDRING_TIDSPUNKT)
                .where(WhereClause.equals(AKTOERID, aktoerId.getValue()).and(
                        WhereClause.equals(SISTE_ENDRING_KATEGORI, kategori.name())
                )).execute();
    }


    public void slettSisteEndringer(AktoerId aktoerId) {
        SqlUtils.delete(jdbcTemplate, TABLE_NAME)
                .where(WhereClause.equals(AKTOERID, aktoerId.getValue())).execute();
    }

    public void setAlleSisteEndringTidspunkter(List<OppfolgingsBruker> oppfolgingsBrukere) {
        if (oppfolgingsBrukere == null || oppfolgingsBrukere.isEmpty()) {
            throw new IllegalArgumentException("Trenger oppfolgingsBrukere for å hente ut siste_endringer");
        }
        for (OppfolgingsBruker bruker : oppfolgingsBrukere) {
            mapDbTilOppfolgingsBruker(bruker);
        }
    }

    @SneakyThrows
    private void mapDbTilOppfolgingsBruker(OppfolgingsBruker oppfolgingsBrukere) {
        oppfolgingsBrukere.setSiste_endringer(jdbcTemplate.query(getAlleKategorierForAktoerId(oppfolgingsBrukere.getAktoer_id()), rs -> {
            Map<String, Endring> sisteEndring = new HashMap<>();
            while(rs.next()){
                sisteEndring.put(rs.getString(SISTE_ENDRING_KATEGORI).toLowerCase(),new Endring()
                                .setTidspunkt(toIsoUTC(rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT)))
                                .setAktivtetId(rs.getString(AKTIVITETID)));
            }
            return sisteEndring;
        }));
    }

    private String getAlleKategorierForAktoerId(String aktorID) {
        return "SELECT " +
                SISTE_ENDRING_KATEGORI + ", " +
                SISTE_ENDRING_TIDSPUNKT + ", " +
                AKTIVITETID +
                " FROM " + TABLE_NAME +
                " WHERE " +
                AKTOERID + "=" + aktorID;
    }
}