package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.domene.Endring;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
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
                .set(AKTOERID, sisteEndringDTO.getAktoerId().get())
                .set(SISTE_ENDRING_KATEGORI, sisteEndringDTO.getKategori().name())
                .set(SISTE_ENDRING_TIDSPUNKT, Timestamp.from(sisteEndringDTO.getTidspunkt().toInstant()))
                .set(AKTIVITETID, sisteEndringDTO.getAktivtetId())
                .where(WhereClause.equals(AKTOERID, sisteEndringDTO.getAktoerId().get()).and(
                        WhereClause.equals(SISTE_ENDRING_KATEGORI, sisteEndringDTO.getKategori().name())
                )).execute();
    }

    public void oppdaterHarSett(AktorId aktorId, SisteEndringsKategori kategori, boolean erSett) {
        String erSettChar = erSett ? "J" : "N";

        SqlUtils.upsert(jdbcTemplate, TABLE_NAME)
                .set(AKTOERID, aktorId.get())
                .set(SISTE_ENDRING_KATEGORI, kategori.name())
                .set(ER_SETT, erSettChar)
                .where(WhereClause.equals(AKTOERID, aktorId.get()).and(
                        WhereClause.equals(SISTE_ENDRING_KATEGORI, kategori.name())
                )).execute();
    }

    public Timestamp getSisteEndringTidspunkt(AktorId aktoerId, SisteEndringsKategori kategori) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT))
                .column(SISTE_ENDRING_TIDSPUNKT)
                .where(WhereClause.equals(AKTOERID, aktoerId.get()).and(
                        WhereClause.equals(SISTE_ENDRING_KATEGORI, kategori.name())
                )).execute();
    }

    public void slettSisteEndringer(AktorId aktoerId) {
        SqlUtils.delete(jdbcTemplate, TABLE_NAME)
                .where(WhereClause.equals(AKTOERID, aktoerId.get())).execute();
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
        oppfolgingsBrukere.setSiste_endringer(getSisteEndringer(AktorId.of(oppfolgingsBrukere.getAktoer_id())));
    }

    public Map<String, Endring> getSisteEndringer(AktorId aktoerId) {
        return jdbcTemplate.query(getAlleKategorierForAktorId(aktoerId.get()), this::mapResultatTilKategoriOgEndring);
    }

    @SneakyThrows
    private Map<String, Endring> mapResultatTilKategoriOgEndring(ResultSet rs){
        Map<String, Endring> sisteEndring = new HashMap<>();
        while(rs.next()){
            sisteEndring.put(rs.getString(SISTE_ENDRING_KATEGORI).toLowerCase(),new Endring()
                    .setTidspunkt(toIsoUTC(rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT)))
                    .setEr_sett(rs.getString(ER_SETT))
                    .setAktivtetId(rs.getString(AKTIVITETID)));
        }
        return sisteEndring;
    }

    private String getAlleKategorierForAktorId(String aktorID) {
        return "SELECT " +
                SISTE_ENDRING_KATEGORI + ", " +
                SISTE_ENDRING_TIDSPUNKT + ", " +
                ER_SETT + ", " +
                AKTIVITETID +
                " FROM " + TABLE_NAME +
                " WHERE " +
                AKTOERID + "=" + aktorID;
    }
}