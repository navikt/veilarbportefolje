package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.domene.EnhetTiltak;
import no.nav.pto.veilarbportefolje.domene.Tiltakkodeverk;
import no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto;
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
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKERTILTAK.*;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.leggTilAktivitetPaResultat;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.mapTiltakTilEntity;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV2 {
    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    public void upsert(TiltakInnhold innhold, AktorId aktorId) {
        LocalDateTime fraDato = getLocalDateTimeOrNull(innhold.getAktivitetperiodeFra(), false);
        LocalDateTime tilDato = getLocalDateTimeOrNull(innhold.getAktivitetperiodeTil(), true);

        log.info("Lagrer tiltak: {}", innhold.getAktivitetid());

        if (skalOppdatereTiltakskodeVerk(innhold.getTiltakstype(), innhold.getTiltaksnavn())) {
            upsertTiltakKodeVerk(innhold);
        }
        db.update("""
                        INSERT INTO brukertiltak
                        (aktivitetid, personid, aktoerid, tiltakskode, fradato, tildato) VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (aktivitetid) DO UPDATE SET (personid, aktoerid, tiltakskode, fradato, tildato)
                        = (excluded.personid, excluded.aktoerid, excluded.tiltakskode, excluded.fradato, excluded.tildato)
                        """,
                innhold.getAktivitetid(),
                String.valueOf(innhold.getPersonId()),
                aktorId.get(),
                innhold.getTiltakstype(),
                fraDato,
                tilDato
        );
    }

    public void delete(String tiltakId) {
        log.info("Sletter tiltak: {}", tiltakId);
        db.update("DELETE FROM brukertiltak WHERE aktivitetid = ?", tiltakId);
    }

    public EnhetTiltak hentTiltakPaEnhet(EnhetId enhetId) {
        final String hentTiltakPaEnhetSql = """
                SELECT * FROM tiltakkodeverket WHERE
                kode IN (SELECT DISTINCT tiltakskode FROM brukertiltak BT
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

    public Optional<String> hentVerdiITiltakskodeVerk(String kode) {
        String sql = "SELECT verdi FROM tiltakkodeverket WHERE kode = ?";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, String.class, kode))
        );
    }

    /*
    private Long getVersjon(String aktivitetId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTIVITETID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getLong(VERSION), aktivitetId))
        ).orElse(-1L);
    }
     */
    //TODO: få inn VERSION som felt i BRUKERTILTAK-TABELLEN


    public void leggTilTiltak(String aktoerIder, HashMap<AktorId, List<AktivitetEntityDto>> result) {
        namedDb.query("""
                        SELECT aktoerid, tildato, fradato, tiltakskode FROM brukertiltak
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

    private void upsertTiltakKodeVerk(TiltakInnhold innhold) {
        db.update("""
                        INSERT INTO tiltakkodeverket (kode, verdi) VALUES (?, ?)
                        ON CONFLICT (kode) DO UPDATE SET verdi = excluded.verdi
                        """,
                innhold.getTiltakstype(), innhold.getTiltaksnavn()
        );
    }

    private boolean skalOppdatereTiltakskodeVerk(String tiltaksKode, String verdiFraKafka) {
        Optional<String> verdiITiltakskodeVerk = hentVerdiITiltakskodeVerk(tiltaksKode);
        return verdiITiltakskodeVerk.map(lagretVerdi -> !lagretVerdi.equals(verdiFraKafka)).orElse(true);
    }

    @SneakyThrows
    private Tiltakkodeverk mapTilTiltak(Map<String, Object> rs) {
        return Tiltakkodeverk.of((String) rs.get(PostgresTable.TILTAKKODEVERK.KODE), (String) rs.get(PostgresTable.TILTAKKODEVERK.VERDI));
    }
}
