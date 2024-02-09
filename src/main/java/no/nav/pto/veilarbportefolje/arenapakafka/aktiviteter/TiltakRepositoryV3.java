package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetIkkeAktivStatuser;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK_V2;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKKODEVERK;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.leggTilAktivitetPaResultat;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.mapTiltakTilEntity;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV3 {

    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    private final static String aktivitetsplanenIkkeAktiveStatuser = Arrays.stream(AktivitetIkkeAktivStatuser.values())
            .map(Enum::name).collect(Collectors.joining(",", "{", "}"));

    public void upsert(TiltakaktivitetEntity tiltakaktivitet, AktorId aktorId) {
        LocalDateTime fraDato = getLocalDateTimeOrNull(tiltakaktivitet.getFraDato(), false);
        LocalDateTime tilDato = getLocalDateTimeOrNull(tiltakaktivitet.getTilDato(), true);

        secureLog.info("Lagrer tiltak: {}", tiltakaktivitet.getAktivitetId());

        if (skalOppdatereTiltakskodeVerk(tiltakaktivitet.getTiltakskode(), tiltakaktivitet.getTiltaksnavn())) {
            upsertTiltakKodeVerk(tiltakaktivitet.getTiltakskode(), tiltakaktivitet.getTiltaksnavn());
        }
        db.update("""
                        INSERT INTO brukertiltak_v2
                        (aktivitetid, aktoerid, tiltakskode, fradato, tildato, version, status) VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (aktivitetid) DO UPDATE SET (aktoerid, tiltakskode, fradato, tildato, version, status)
                        = (excluded.aktoerid, excluded.tiltakskode, excluded.fradato, excluded.tildato, excluded.version, excluded.status)
                        """,
                tiltakaktivitet.getAktivitetId(), aktorId.get(), tiltakaktivitet.getTiltakskode(), fraDato, tilDato, tiltakaktivitet.getVersion(), tiltakaktivitet.getStatus()
        );
    }

    public void upsert(TiltakInnhold innhold, AktorId aktorId) {
        LocalDateTime fraDato = getLocalDateTimeOrNull(innhold.getAktivitetperiodeFra(), false);
        LocalDateTime tilDato = getLocalDateTimeOrNull(innhold.getAktivitetperiodeTil(), true);

        secureLog.info("Lagrer tiltak: {}", innhold.getAktivitetid());

        if (skalOppdatereTiltakskodeVerk(innhold.getTiltakstype(), innhold.getTiltaksnavn())) {
            upsertTiltakKodeVerk(innhold.getTiltakstype(), innhold.getTiltaksnavn());
        }
        db.update("""
                        INSERT INTO brukertiltak
                        (aktivitetid, personid, aktoerid, tiltakskode, fradato, tildato) VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (aktivitetid) DO UPDATE SET (personid, aktoerid, tiltakskode, fradato, tildato)
                        = (excluded.personid, excluded.aktoerid, excluded.tiltakskode, excluded.fradato, excluded.tildato)
                        """,
                innhold.getAktivitetid(),
                String.valueOf(innhold.getPersonId()), aktorId.get(), innhold.getTiltakstype(), fraDato, tilDato
        );
    }

    public void deleteTiltaksaktivitetFraAktivitetsplanen(String tiltaksaktivitetId) {
        secureLog.info("Sletter tiltak: {}", tiltaksaktivitetId);
        db.update("DELETE FROM brukertiltak_v2 WHERE aktivitetid = ?", tiltaksaktivitetId);
    }

    public void deleteTiltaksaktivitetFraArena(String tiltaksaktivitetId) {
        secureLog.info("Sletter tiltak: {}", tiltaksaktivitetId);
        db.update("DELETE FROM brukertiltak WHERE aktivitetid = ?", tiltaksaktivitetId);
    }


    public EnhetTiltak hentTiltakPaEnhet(EnhetId enhetId) {
        final String hentTiltakPaEnhetSql = """
                SELECT * FROM tiltakkodeverket WHERE
                kode IN (SELECT DISTINCT tiltakskode FROM
                (
                    SELECT tiltakskode, aktoerid FROM brukertiltak
                    UNION
                    SELECT tiltakskode, aktoerid FROM brukertiltak_v2 WHERE NOT (status = ANY (?::varchar[]))
                ) BT
                INNER JOIN aktive_identer ai on ai.aktorid = BT.aktoerid
                INNER JOIN oppfolgingsbruker_arena_v2 OP ON OP.fodselsnr = ai.fnr
                WHERE OP.nav_kontor = ?)
                """;
        return new EnhetTiltak().setTiltak(
                dbReadOnly.queryForList(hentTiltakPaEnhetSql, aktivitetsplanenIkkeAktiveStatuser, enhetId.get())
                        .stream().map(this::mapTilTiltak)
                        .collect(toMap(Tiltakkodeverk::getKode, Tiltakkodeverk::getVerdi))
        );
    }

    public Optional<String> hentTiltaksnavn(String tiltakskode) {
        String sql = String.format("SELECT verdi FROM %s WHERE %s = ?", TILTAKKODEVERK.TABLE_NAME, TILTAKKODEVERK.KODE);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, String.class, tiltakskode))
        );
    }

    public void leggTilTiltak(String aktoerIder, HashMap<AktorId, List<AktivitetEntityDto>> result) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", aktoerIder);
        params.addValue("ikkestatuser", aktivitetsplanenIkkeAktiveStatuser);

        namedDb.query("""
                        SELECT aktoerid, tildato, fradato, tiltakskode FROM brukertiltak
                        WHERE aktoerid = ANY (:ids::varchar[])
                        UNION
                        SELECT aktoerid, tildato, fradato, tiltakskode FROM brukertiltak_v2
                        WHERE aktoerid = ANY (:ids::varchar[])
                        AND NOT (status = ANY (:ikkestatuser::varchar[]))
                        """,
                params,
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

    public void upsertTiltakKodeVerk(String tiltakskode, String tiltaksnavn) {
        db.update("""
                        INSERT INTO tiltakkodeverket (kode, verdi) VALUES (?, ?)
                        ON CONFLICT (kode) DO UPDATE SET verdi = excluded.verdi
                        """,
                tiltakskode, tiltaksnavn
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

    public void slettOppfolgingData(AktorId aktorId) {
        db.update(String.format("DELETE FROM %s WHERE %s = ?", BRUKERTILTAK.TABLE_NAME, BRUKERTILTAK.AKTOERID), aktorId.get());
        db.update(String.format("DELETE FROM %s WHERE %s = ?", BRUKERTILTAK_V2.TABLE_NAME, BRUKERTILTAK_V2.AKTOERID), aktorId.get());
    }

    @SneakyThrows
    private Tiltakkodeverk mapTilTiltak(Map<String, Object> rs) {
        return Tiltakkodeverk.of((String) rs.get(TILTAKKODEVERK.KODE), (String) rs.get(TILTAKKODEVERK.VERDI));
    }
}
