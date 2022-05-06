package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.domene.Motedeltaker;
import no.nav.pto.veilarbportefolje.domene.Moteplan;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.AKTIVITETID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.AKTIVITETTYPE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.FRADATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.STATUS;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.TILDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.VERSION;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.leggTilAktivitetPaResultat;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.mapAktivitetTilEntity;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AktiviteterRepositoryV2 {
    private final static String aktivitetsplanenIkkeAktiveStatuser = Arrays.stream(AktivitetIkkeAktivStatuser.values())
            .map(Enum::name).collect(Collectors.joining(",", "{", "}"));
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    @Transactional
    public boolean tryLagreAktivitetData(KafkaAktivitetMelding aktivitet) {
        if (aktivitet.isHistorisk()) {
            deleteById(aktivitet.getAktivitetId());
            return true;
        } else if (erNyVersjonAvAktivitet(aktivitet)) {
            upsertAktivitet(aktivitet);
            return true;
        }
        return false;
    }

    public void upsertAktivitet(KafkaAktivitetMelding aktivitet) {
        db.update("""
                        INSERT INTO aktiviteter
                        (AKTIVITETID, AKTOERID, AKTIVITETTYPE, AVTALT , FRADATO, TILDATO, STATUS, VERSION)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (AKTIVITETID)
                        DO UPDATE SET (AKTOERID, AKTIVITETTYPE, AVTALT, FRADATO, TILDATO, STATUS, VERSION) =
                        (excluded.aktoerid, excluded.aktivitettype, excluded.avtalt, excluded.fradato, excluded.tildato, excluded.status, excluded.version)
                        """,
                aktivitet.getAktivitetId(), aktivitet.getAktorId(), aktivitet.getAktivitetType().name().toLowerCase(), aktivitet.isAvtalt(),
                toTimestamp(aktivitet.getFraDato()), toTimestamp(aktivitet.getTilDato()), aktivitet.getAktivitetStatus().name().toLowerCase(), aktivitet.getVersion()
        );
    }

    public void deleteById(String aktivitetid) {
        log.info("Sletter alle aktiviteter med id {}", aktivitetid);
        db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTIVITETID), aktivitetid);
    }

    public void leggTilAktiviteterFraAktivitetsplanen(String aktoerIder, boolean avtalt, HashMap<AktorId, List<AktivitetEntityDto>> result) {
        var params = new MapSqlParameterSource();
        params.addValue("ikkestatuser", aktivitetsplanenIkkeAktiveStatuser);
        params.addValue("avtalt", avtalt);
        params.addValue("ids", aktoerIder);
        namedDb.query("""
                        SELECT aktoerid, tildato, fradato, aktivitettype FROM aktiviteter
                        WHERE avtalt = :avtalt::boolean AND NOT (status = ANY (:ikkestatuser::varchar[])) AND aktoerid = ANY (:ids::varchar[])
                        """,
                params, (ResultSet rs) -> {
                    while (rs.next()) {
                        AktorId aktoerId = AktorId.of(rs.getString("aktoerid"));
                        mapAktivitetTilEntity(rs).ifPresent(aktivitet -> {
                                    List<AktivitetEntityDto> list = result.get(aktoerId);
                                    result.put(aktoerId, leggTilAktivitetPaResultat(aktivitet, list));
                                }
                        );
                    }
                    return result;
                });
    }

    public List<AktivitetDTO> getPasserteAktiveUtdanningsAktiviter() {
        final String sql = """
                SELECT * FROM aktiviteter
                WHERE aktivitettype = 'utdanningaktivitet'
                AND NOT status = 'fullfort'
                AND date_trunc('day', tildato) < date_trunc('day', current_timestamp)
                """;
        return db.query(sql, this::mapToAktivitetDTOList);
    }

    public void setTilFullfort(String aktivitetid) {
        log.info("Setter status flagget til aktivitet: {}, til verdien fullfort", aktivitetid);
        db.update("UPDATE aktiviteter SET status = 'fullfort' WHERE aktivitetid = ?", aktivitetid);
    }

    public List<Moteplan> hentFremtidigeMoter(VeilederId veilederIdent, EnhetId enhet) {
        List<Moteplan> result = new ArrayList<>();

        var params = new MapSqlParameterSource();
        params.addValue("ikkestatuser", aktivitetsplanenIkkeAktiveStatuser);
        params.addValue("veilederIdent", veilederIdent.getValue());
        params.addValue("enhet", enhet.get());
        return namedDb.query("""
                        SELECT op.fodselsnr, op.fornavn, op.etternavn, a.fradato, a.avtalt
                         from oppfolgingsbruker_arena_v2 op
                        inner join aktive_identer ai on op.fodselsnr = ai.fnr
                        inner join oppfolging_data od on od.aktoerid = ai.aktorid
                        inner join aktiviteter a on a.aktoerid = ai.aktorid
                        where op.nav_kontor = :enhet::varchar
                        AND od.veilederid = :veilederIdent::varchar
                        AND a.aktivitettype = 'mote'
                        AND date_trunc('day', tildato) >= date_trunc('day', current_timestamp)
                        AND NOT (status = ANY (:ikkestatuser::varchar[]))
                        ORDER BY a.fradato
                        """,
                params, (ResultSet rs) -> {
                    while (rs.next()) {
                        result.add(mapTilMoteplan(rs));
                    }
                    return result;
                });
    }

    @SneakyThrows
    private Moteplan mapTilMoteplan(ResultSet rs) {
        return new Moteplan(
                new Motedeltaker(rs.getString("fornavn"), rs.getString("etternavn"),
                        rs.getString("fodselsnr")),
                toIsoUTC(rs.getTimestamp("fradato")),
                rs.getBoolean("avtalt")
        );
    }

    private boolean erNyVersjonAvAktivitet(KafkaAktivitetMelding aktivitet) {
        Long kommendeVersjon = aktivitet.getVersion();
        if (kommendeVersjon == null) {
            return false;
        }
        Long databaseVersjon = getVersjon(aktivitet.getAktivitetId());
        if (databaseVersjon == null) {
            return true;
        }
        return kommendeVersjon.compareTo(databaseVersjon) >= 0;
    }

    private Long getVersjon(String aktivitetId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTIVITETID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getLong(VERSION), aktivitetId))
        ).orElse(-1L);
    }

    @SneakyThrows
    private List<AktivitetDTO> mapToAktivitetDTOList(ResultSet rs) {
        List<AktivitetDTO> aktiviteter = new ArrayList<>();
        while (rs.next()) {
            aktiviteter.add(new AktivitetDTO()
                    .setAktivitetID(rs.getString(AKTIVITETID))
                    .setAktivitetType(rs.getString(AKTIVITETTYPE))
                    .setStatus(rs.getString(STATUS))
                    .setFraDato(rs.getTimestamp(FRADATO))
                    .setTilDato(rs.getTimestamp(TILDATO)));
        }
        return aktiviteter;
    }
}
