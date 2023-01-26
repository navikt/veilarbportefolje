package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.Tiltakkodeverk;
import no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto;
import no.nav.pto.veilarbportefolje.postgres.utils.TiltakaktivitetEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.leggTilAktivitetPaResultat;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.mapTiltakTilEntity;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV3 {

    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    public void upsert(TiltakaktivitetEntity tiltakaktivitet, AktorId aktorId) {
        LocalDateTime fraDato = getLocalDateTimeOrNull(tiltakaktivitet.getFraDato(), false);
        LocalDateTime tilDato = getLocalDateTimeOrNull(tiltakaktivitet.getTilDato(), true);

        log.info("Lagrer tiltak: {}", tiltakaktivitet.getAktivitetId());

        if (skalOppdatereTiltakskodeVerk(tiltakaktivitet.getTiltakskode(), tiltakaktivitet.getTiltaksnavn())) {
            upsertTiltakKodeVerk(tiltakaktivitet);
        }
        db.update("""
                        INSERT INTO brukertiltak_v2
                        (aktivitetid, aktoerid, tiltakskode, fradato, tildato, version) VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (aktivitetid) DO UPDATE SET (aktoerid, tiltakskode, fradato, tildato, version)
                        = (excluded.aktoerid, excluded.tiltakskode, excluded.fradato, excluded.tildato, excluded.version)
                        """,
                tiltakaktivitet.getAktivitetId(), aktorId.get(), tiltakaktivitet.getTiltakskode(), fraDato, tilDato, tiltakaktivitet.getVersion()
        );
    }

    public void delete(String tiltakaktivitetId) {
        log.info("Sletter tiltak: {}", tiltakaktivitetId);
        db.update("DELETE FROM brukertiltak_v2 WHERE aktivitetid = ?", tiltakaktivitetId);
    }

    public EnhetTiltak hentTiltakPaEnhet(EnhetId enhetId) {
        final String hentTiltakPaEnhetSql = """
                SELECT * FROM tiltakkodeverket WHERE
                kode IN (SELECT DISTINCT tiltakskode FROM
                (
                    SELECT tiltakskode, aktoerid FROM brukertiltak
                    UNION
                    SELECT tiltakskode, aktoerid FROM brukertiltak_v2
                ) BT
                INNER JOIN aktive_identer ai on ai.aktorid = BT.aktoerid
                INNER JOIN oppfolgingsbruker_arena_v2 OP ON OP.fodselsnr = ai.fnr
                WHERE OP.nav_kontor = ?)
                """;
        return new EnhetTiltak().setTiltak(
                dbReadOnly.queryForList(hentTiltakPaEnhetSql, enhetId.get())
                        .stream().map(this::mapTilTiltak)
                        .collect(toMap(Tiltakkodeverk::getKode, Tiltakkodeverk::getVerdi))
        );
    }

    public Optional<String> hentTiltaksnavn(String tiltakskode) {
        String sql = String.format("SELECT verdi FROM %s WHERE %s = ?", PostgresTable.TILTAKKODEVERK.TABLE_NAME, PostgresTable.TILTAKKODEVERK.KODE);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, String.class, tiltakskode))
        );
    }

    public void leggTilTiltak(String aktoerIder, HashMap<AktorId, List<AktivitetEntityDto>> result) {
        namedDb.query("""
                        SELECT aktoerid, tildato, fradato, tiltakskode FROM brukertiltak
                        WHERE aktoerid = ANY (:ids::varchar[])
                        UNION
                        SELECT aktoerid, tildato, fradato, tiltakskode FROM brukertiltak_v2
                        WHERE aktoerid = ANY (:ids::varchar[])
                        """,
                new MapSqlParameterSource("ids", aktoerIder),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        AktorId aktoerId = AktorId.of(rs.getString("aktoerid"));
                        AktivitetEntityDto aktivitet = mapTiltakTilEntity(rs);

                        List<AktivitetEntityDto> list = result.get(aktoerId);
                        result.put(aktoerId, leggTilAktivitetPaResultat(aktivitet, list));
                    }
                    return result;
                });
    }

    public void upsertTiltakKodeVerk(TiltakaktivitetEntity innhold) {
        db.update("""
                        INSERT INTO tiltakkodeverket (kode, verdi) VALUES (?, ?)
                        ON CONFLICT (kode) DO UPDATE SET verdi = excluded.verdi
                        """,
                innhold.getTiltakskode(), innhold.getTiltaksnavn()
        );
    }

    public Long hentVersjon(String aktivitetId) {
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(
                        "SELECT * FROM brukertiltak_v2 WHERE aktivitetid = ?",
                        (rs, row) -> rs.getLong("version"),
                        aktivitetId
                ))
        ).orElse(-1L);
    }

    public boolean skalOppdatereTiltakskodeVerk(String tiltaksKode, String verdiFraKafka) {
        Optional<String> verdiITiltakskodeVerk = hentTiltaksnavn(tiltaksKode);
        return verdiITiltakskodeVerk.map(lagretVerdi -> !lagretVerdi.equals(verdiFraKafka)).orElse(true);
    }

    @SneakyThrows
    private Tiltakkodeverk mapTilTiltak(Map<String, Object> rs) {
        return Tiltakkodeverk.of((String) rs.get(PostgresTable.TILTAKKODEVERK.KODE), (String) rs.get(PostgresTable.TILTAKKODEVERK.VERDI));
    }


}
