package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSLISTE.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArbeidslisteRepositoryV2 {
    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;


    public Optional<String> hentNavKontorForArbeidsliste(AktorId aktorId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s=? ", NAV_KONTOR_FOR_ARBEIDSLISTE, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getString(NAV_KONTOR_FOR_ARBEIDSLISTE), aktorId.get()))
        );
    }

    public Try<Arbeidsliste> retrieveArbeidsliste(AktorId aktorId) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, AKTOERID);
        return Try.of(
                () -> queryForObjectOrNull(
                        () -> db.queryForObject(sql, this::arbeidslisteMapper, aktorId.get())
                )
        );
    }

    // Ny metode
    public Try<Arbeidsliste> retrieveArbeidsliste(Fnr bruker) {
        String sql = """
                    SELECT a.*, f.verdi FROM arbeidsliste a
                    INNER JOIN aktive_identer ai on ai.aktorid = a.aktoerid
                    LEFT JOIN fargekategori f on f.fnr = ai.fnr
                    WHERE ai.fnr = ?
                """;
        return Try.of(
                () -> queryForObjectOrNull(
                        () -> db.queryForObject(sql, this::arbeidslisteMapper, bruker.get())
                )
        );
    }

    public List<Arbeidsliste> hentArbeidslisteForVeilederPaEnhet(EnhetId enhet, VeilederId veilederident) {
        return dbReadOnly.queryForList("""
                                SELECT a.* FROM arbeidsliste a
                                INNER JOIN oppfolging_data o ON a.aktoerid = o.aktoerid
                                INNER JOIN aktive_identer ai on ai.aktorid = a.aktoerid
                                INNER JOIN oppfolgingsbruker_arena_v2 ob on ai.fnr = ob.fodselsnr
                                WHERE ob.nav_kontor = ?
                                AND o.veilederid = ?""",
                        enhet.get(),
                        veilederident.getValue()
                )
                .stream()
                .map(ArbeidslisteRepositoryV2::arbeidslisteMapper)
                .toList();
    }


    public Try<ArbeidslisteDTO> insertArbeidsliste(ArbeidslisteDTO dto) {
        return Try.of(
                () -> {
                    dto.setEndringstidspunkt(Timestamp.from(now()));
                    AktorId aktoerId = Optional
                            .ofNullable(dto.getAktorId())
                            .orElseThrow(() -> new RuntimeException("Fant ikke aktør-ID"));
                    dto.setAktorId(aktoerId);

                    upsert(aktoerId.get(), dto);
                    return dto;
                }
        ).onFailure(e -> secureLog.warn("Kunne ikke inserte arbeidsliste til db", e));
    }


    public Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data) {
        final String updateSql = String.format(
                "UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ?",
                TABLE_NAME, SIST_ENDRET_AV_VEILEDERIDENT, ENDRINGSTIDSPUNKT, OVERSKRIFT,
                KOMMENTAR, FRIST, KATEGORI, AKTOERID
        );

        Timestamp endringsTidspunkt = Timestamp.from(now());
        return Try.of(
                () -> {
                    int rows = db.update(updateSql, data.getVeilederId().getValue(), endringsTidspunkt, data.getOverskrift(),
                            data.getKommentar(), data.getFrist(), data.getKategori().name(), data.getAktorId().get());

                    secureLog.info("Oppdaterte arbeidsliste pa bruker {}, rader: {}", data.getAktorId().get(), rows);
                    return data.setEndringstidspunkt(endringsTidspunkt);
                }
        ).onFailure(e -> secureLog.warn("Kunne ikke oppdatere arbeidsliste i db", e));
    }

    public int slettArbeidsliste(AktorId aktoerId) {
        if (aktoerId == null) {
            return 0;
        }
        secureLog.info("Sletter arbeidsliste pa bruker: {}", aktoerId);
        return db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktoerId.get());
    }

    public int upsert(String aktoerId, ArbeidslisteDTO dto) {
        secureLog.info("Upsert arbeidsliste pa bruker: {}", aktoerId);
        Optional<Arbeidsliste.Kategori> maybeKategori = Optional.ofNullable(dto.getKategori());
        return db.update("""
                        INSERT INTO ARBEIDSLISTE (AKTOERID, SIST_ENDRET_AV_VEILEDERIDENT , ENDRINGSTIDSPUNKT,
                        OVERSKRIFT, KOMMENTAR, FRIST , KATEGORI, NAV_KONTOR_FOR_ARBEIDSLISTE)
                        VALUES(?,?,?,?,?,?,?,?) ON CONFLICT (AKTOERID) DO UPDATE SET
                        (SIST_ENDRET_AV_VEILEDERIDENT, ENDRINGSTIDSPUNKT, OVERSKRIFT, KOMMENTAR , FRIST , KATEGORI, NAV_KONTOR_FOR_ARBEIDSLISTE) =
                        (excluded.SIST_ENDRET_AV_VEILEDERIDENT, excluded.ENDRINGSTIDSPUNKT, excluded.OVERSKRIFT, excluded.KOMMENTAR , excluded.FRIST , excluded.KATEGORI, excluded.NAV_KONTOR_FOR_ARBEIDSLISTE)
                        """,
                aktoerId, dto.getVeilederId().getValue(), dto.getEndringstidspunkt(), dto.getOverskrift(), dto.getKommentar(), dto.getFrist(), maybeKategori.map((Enum::toString)).orElse(null), dto.getNavKontorForArbeidsliste());
    }

    @SneakyThrows
    private Arbeidsliste arbeidslisteMapper(ResultSet rs, int row) {
        String kategoriFraFargekategoriTabell = rs.getString("VERDI");
        String kategoriFraArbeidslisteTabell = rs.getString("KATEGORI");

        return new Arbeidsliste(
                VeilederId.of(rs.getString(SIST_ENDRET_AV_VEILEDERIDENT)),
                toZonedDateTime(rs.getTimestamp(ENDRINGSTIDSPUNKT)),
                rs.getString(OVERSKRIFT),
                rs.getString(KOMMENTAR),
                toZonedDateTime(rs.getTimestamp(FRIST)),
                kategoriFraFargekategoriTabell != null ?
                        Arbeidsliste.Kategori.valueOf(kategoriFraFargekategoriTabell)
                        : Arbeidsliste.Kategori.valueOf(kategoriFraArbeidslisteTabell)
        ).setAktoerid(rs.getString(AKTOERID));
    }

    @SneakyThrows
    private static Arbeidsliste arbeidslisteMapper(Map<String, Object> rs) {
        return new Arbeidsliste(
                VeilederId.of((String) rs.get(SIST_ENDRET_AV_VEILEDERIDENT)),
                toZonedDateTime((Timestamp) rs.get(ENDRINGSTIDSPUNKT)),
                (String) rs.get(OVERSKRIFT),
                (String) rs.get(KOMMENTAR),
                toZonedDateTime((Timestamp) rs.get(FRIST)),
                Arbeidsliste.Kategori.valueOf((String) rs.get(KATEGORI))
        ).setAktoerid((String) rs.get(AKTOERID));
    }
}
