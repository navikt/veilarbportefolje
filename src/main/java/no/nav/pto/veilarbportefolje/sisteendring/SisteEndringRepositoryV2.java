package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.Aktorid_indeksert_data.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.Table.SISTE_ENDRING.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.DbUtils.boolToJaNei;

@Slf4j
@Repository
public class SisteEndringRepositoryV2 {
    @NonNull
    private final JdbcTemplate db;

    @Autowired
    public SisteEndringRepositoryV2(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public int upsert(SisteEndringDTO sisteEndringDTO) {
        return db.update(
                "INSERT INTO " + TABLE_NAME +
                        "(" +
                        AKTOERID + "," +
                        SISTE_ENDRING_KATEGORI + "," +
                        SISTE_ENDRING_TIDSPUNKT + "," +
                        AKTIVITETID + "," +
                        ER_SETT +
                        ") VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTOERID + ", " + SISTE_ENDRING_KATEGORI + ") " +
                        "DO UPDATE SET (" +
                        SISTE_ENDRING_TIDSPUNKT + "," +
                        AKTIVITETID + "," +
                        ER_SETT + ") = (?, ?, ?)",
                sisteEndringDTO.getAktoerId().get(),
                sisteEndringDTO.getKategori().name(),
                Timestamp.from(sisteEndringDTO.getTidspunkt().toInstant()),
                sisteEndringDTO.getAktivtetId(),
                false,
                Timestamp.from(sisteEndringDTO.getTidspunkt().toInstant()),
                sisteEndringDTO.getAktivtetId(),
                false
        );
    }

    public int oppdaterHarSett(AktorId aktorId, SisteEndringsKategori kategori, boolean erSett) {
        return db.update(
                "INSERT INTO " + TABLE_NAME +
                        "(" +
                        AKTOERID + "," +
                        SISTE_ENDRING_KATEGORI + "," +
                        ER_SETT +
                        ") VALUES (?, ?, ?) " +
                        "ON CONFLICT (" + AKTOERID + ", " + SISTE_ENDRING_KATEGORI + ") " +
                        "DO UPDATE SET " +
                        ER_SETT + " = (?)",
                aktorId.get(),
                kategori.name(),
                erSett,
                erSett
        );


    }

    public Timestamp getSisteEndringTidspunkt(AktorId aktoerId, SisteEndringsKategori kategori) {
        return queryForObjectOrNull(() -> db.queryForObject("""
                        SELECT SISTE_ENDRING_TIDSPUNKT FROM SISTE_ENDRING
                        WHERE aktoerid = ? AND siste_endring_kategori = ?""",
                Timestamp.class, aktoerId.get(), kategori.name())
        );
    }

    public int slettSisteEndringer(AktorId aktoerId) {
        String sql = String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);

        return db.update(sql, aktoerId.get());
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
        String sql = String.format("SELECT " +
                SISTE_ENDRING_KATEGORI + ", " +
                SISTE_ENDRING_TIDSPUNKT + ", " +
                ER_SETT + ", " +
                AKTIVITETID +
                " FROM " + TABLE_NAME +
                " WHERE " +
                AKTOERID + "= ?");
        return db.query(sql, this::mapResultatTilKategoriOgEndring, aktoerId.get());
    }

    public Map<AktorId, Map<String, Endring>> getSisteEndringer(List<AktorId> aktoerIder) {
        String aktoerIderStr = aktoerIder.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));

        String sql = String.format("SELECT " +
                SISTE_ENDRING_KATEGORI + ", " +
                SISTE_ENDRING_TIDSPUNKT + ", " +
                ER_SETT + ", " +
                AKTIVITETID +
                " FROM " + TABLE_NAME +
                " WHERE " +
                AKTOERID + " = ANY (?::varchar[])");
        return db.query(sql,
                ps -> ps.setString(1, aktoerIderStr),
                (ResultSet rs) -> {
                    HashMap<AktorId, Map<String, Endring>> results = new HashMap<>();
                    while (rs.next()) {
                        results.put(AktorId.of(rs.getString(AKTOERID)), mapResultatTilKategoriOgEndring(rs));
                    }
                    return results;
                });
    }

    @SneakyThrows
    private Map<String, Endring> mapResultatTilKategoriOgEndring(ResultSet rs) {
        Map<String, Endring> sisteEndring = new HashMap<>();
        while (rs.next()) {
            sisteEndring.put(rs.getString(SISTE_ENDRING_KATEGORI), new Endring()
                    .setTidspunkt(toIsoUTC(rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT)))
                    .setEr_sett(boolToJaNei(rs.getBoolean(ER_SETT)))
                    .setAktivtetId(rs.getString(AKTIVITETID)));
        }
        return sisteEndring;
    }
}