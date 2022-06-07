package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.opensearch.domene.Endring;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.SISTE_ENDRING.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.DbUtils.boolToJaNei;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SisteEndringRepositoryV2 {
    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    public int upsert(SisteEndringDTO sisteEndringDTO) {
        return db.update("""
                        insert into siste_endring (aktoerid, siste_endring_kategori, siste_endring_tidspunkt, aktivitetid, er_sett)
                        values (?, ?, ?, ?, false) on conflict (aktoerid, siste_endring_kategori)
                        do update set (siste_endring_tidspunkt, aktivitetid, er_sett)
                        = (excluded.siste_endring_tidspunkt, excluded.aktivitetid, excluded.er_sett)
                        """,
                sisteEndringDTO.getAktoerId().get(),
                sisteEndringDTO.getKategori().name(),
                Timestamp.from(sisteEndringDTO.getTidspunkt().toInstant()),
                sisteEndringDTO.getAktivtetId()
        );
    }

    public void oppdaterHarSett(AktorId aktorId, SisteEndringsKategori kategori, boolean erSett) {
        db.update(
                """
                        insert into siste_endring (aktoerid, siste_endring_kategori, er_sett)
                        values (?, ?, ?)
                        on conflict (aktoerid, siste_endring_kategori)
                        do update set er_sett = excluded.er_sett
                        """,
                aktorId.get(), kategori.name(), erSett
        );
    }

    public Timestamp getSisteEndringTidspunkt(AktorId aktoerId, SisteEndringsKategori kategori) {
        return queryForObjectOrNull(() -> dbReadOnly.queryForObject("""
                        SELECT SISTE_ENDRING_TIDSPUNKT FROM SISTE_ENDRING
                        WHERE aktoerid = ? AND siste_endring_kategori = ?""",
                Timestamp.class, aktoerId.get(), kategori.name())
        );
    }

    public void slettSisteEndringer(AktorId aktoerId) {
        db.update("delete from siste_endring where aktoerid = ?", aktoerId.get());
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
        return dbReadOnly.query( """
                select siste_endring_kategori, siste_endring_tidspunkt, er_sett, aktivitetid from siste_endring
                where aktoerid = ?""", this::mapResultatTilKategoriOgEndring,
                aktoerId.get());
    }

    public Map<AktorId, Map<String, Endring>> getSisteEndringer(List<AktorId> aktoerIder) {
        String aktoerIderStr = aktoerIder.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));

        String sql = """
                SELECT AKTOERID, SISTE_ENDRING_KATEGORI, SISTE_ENDRING_TIDSPUNKT, ER_SETT, AKTIVITETID
                FROM SISTE_ENDRING WHERE AKTOERID = ANY (?::varchar[])
                """;
        return dbReadOnly.query(sql,
                ps -> ps.setString(1, aktoerIderStr),
                (ResultSet rs) -> {
                    HashMap<AktorId, Map<String, Endring>> results = new HashMap<>();
                    while (rs.next()) {
                        AktorId aktorId = AktorId.of(rs.getString(AKTOERID));
                        Map<String, Endring> sisteEndringerForBruker = results.getOrDefault(aktorId, new HashMap<>());
                        results.put(aktorId, addSisteEndringer(sisteEndringerForBruker, rs));
                    }
                    return results;
                });
    }

    @SneakyThrows
    private Map<String, Endring> mapResultatTilKategoriOgEndring(ResultSet rs) {
        HashMap<String, Endring> sisteEndringHashMap = new HashMap<>();
        while (rs.next()) {
            addSisteEndringer(sisteEndringHashMap, rs);
        }
        return sisteEndringHashMap;
    }

    @SneakyThrows
    private Map<String, Endring> addSisteEndringer(Map<String, Endring> sisteEndringHashMap, ResultSet rs) {
        sisteEndringHashMap.put(rs.getString(SISTE_ENDRING_KATEGORI),
                new Endring()
                        .setTidspunkt(toIsoUTC(rs.getTimestamp(SISTE_ENDRING_TIDSPUNKT)))
                        .setEr_sett(boolToJaNei(rs.getBoolean(ER_SETT)))
                        .setAktivtetId(rs.getString(AKTIVITETID)));

        return sisteEndringHashMap;
    }
}
