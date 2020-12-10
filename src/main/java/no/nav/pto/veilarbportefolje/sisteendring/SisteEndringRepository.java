package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.DbUtils;
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.database.Table.SISTE_ENDRING.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.iso8601FromTimestamp;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parse0OR1;

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
                )).execute();
    }

    public ResultSet getAlleSisteEndringTidspunkter(AktoerId aktoerId) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs)
                .column(SISTE_ENDRING_TIDSPUNKT)
                .column(SISTE_ENDRING_KATEGORI)
                .where(WhereClause.equals(AKTOERID, aktoerId.getValue())).execute();
    }

    public void setAlleSisteEndringTidspunkter(List<OppfolgingsBruker> oppfolgingsBrukere) {
        if (oppfolgingsBrukere == null || oppfolgingsBrukere.isEmpty()) {
            throw new IllegalArgumentException("Trenger oppfolgingsBrukere for å hente ut siste_endringer");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("aktoerider", oppfolgingsBrukere.stream().map(OppfolgingsBruker::getAktoer_id).collect(toList()));
        for (OppfolgingsBruker bruker : oppfolgingsBrukere) {
            mapDbTilOppfolgingsBruker(bruker);
        }
        System.out.println("s");
        //        .forEach(a -> mapDbTilOppfolgingsBruker(a, oppfolgingsBrukere));
    }

    @SneakyThrows
    private void mapDbTilOppfolgingsBruker(OppfolgingsBruker oppfolgingsBrukere) {
        jdbcTemplate.query("select * from books", new RowCallbackHandler() {
            public void processRow(ResultSet resultSet) throws SQLException {
                while (resultSet.next()) {
                    String name = resultSet.getString("Name");
                    // process it
                }
            }
        });
        String q = getOppfolgingsBrukerForListeAvAktoerId(oppfolgingsBrukere.getAktoer_id());
        ResultSet rs = getOppfolgingsBrukerForListeAvAktoerId(AktoerId.of());
        while (rs.next()) {
            Timestamp coffeeName = rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT);
            String kategori = rs.getString(SISTE_ENDRING_KATEGORI);
            System.out.println(coffeeName + ", " + kategori);
        }
    /*
        oppfolgingsBruker.setSiste_endring_mal(iso8601FromTimestamp((Timestamp) row.get(MAL)));

        oppfolgingsBruker.setSiste_endring_ny_stilling(iso8601FromTimestamp((Timestamp) row.get(NY_STILLING)));
        oppfolgingsBruker.setSiste_endring_ny_ijobb(iso8601FromTimestamp((Timestamp) row.get(NY_IJOBB)));
        oppfolgingsBruker.setSiste_endring_ny_egen(iso8601FromTimestamp((Timestamp) row.get(NY_EGEN)));
        oppfolgingsBruker.setSiste_endring_ny_behandling(iso8601FromTimestamp((Timestamp) row.get(NY_BEHANDLING)));

        oppfolgingsBruker.setSiste_endring_fullfort_stilling(iso8601FromTimestamp((Timestamp) row.get(FULLFORT_STILLING)));
        oppfolgingsBruker.setSiste_endring_fullfort_ijobb(iso8601FromTimestamp((Timestamp) row.get(FULLFORT_IJOBB)));
        oppfolgingsBruker.setSiste_endring_fullfort_egen(iso8601FromTimestamp((Timestamp) row.get(FULLFORT_EGEN)));
        oppfolgingsBruker.setSiste_endring_fullfort_behandling(iso8601FromTimestamp((Timestamp) row.get(FULLFORT_BEHANDLING)));

        oppfolgingsBruker.setSiste_endring_avbrutt_stilling(iso8601FromTimestamp((Timestamp) row.get(AVBRUTT_STILLING)));
        oppfolgingsBruker.setSiste_endring_avbrutt_ijobb(iso8601FromTimestamp((Timestamp) row.get(AVBRUTT_IJOBB)));
        oppfolgingsBruker.setSiste_endring_avbrutt_egen(iso8601FromTimestamp((Timestamp) row.get(AVBRUTT_EGEN)));
        oppfolgingsBruker.setSiste_endring_avbrutt_behandling(iso8601FromTimestamp((Timestamp) row.get(AVBRUTT_BEHANDLING)));
    */
    }

    private String getOppfolgingsBrukerForListeAvAktoerId(String aktorID) {
        return      "SELECT " +
                    SISTE_ENDRING_KATEGORI + ", " +
                    SISTE_ENDRING_TIDSPUNKT +
                    " FROM " + TABLE_NAME +
                    " WHERE " +
                    AKTOERID+"="+aktorID;
    }

}


