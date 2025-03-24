package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA.MANUELL;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA.NY_FOR_VEILEDER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA.OPPFOLGING;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA.STARTDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA.VEILEDERID;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OppfolgingRepositoryV2 {
    private final JdbcTemplate db;

    public int settUnderOppfolging(AktorId aktoerId, ZonedDateTime startDato) {
        return db.update("""
                INSERT INTO oppfolging_data (AKTOERID, OPPFOLGING, STARTDATO) VALUES (?,?,?)
                ON CONFLICT (AKTOERID) DO UPDATE SET OPPFOLGING = EXCLUDED.OPPFOLGING, STARTDATO = EXCLUDED.STARTDATO
                """, aktoerId.get(), true, toTimestamp(startDato)
        );
    }

    public void settVeileder(AktorId aktorId, VeilederId veilederId) {
        db.update("UPDATE oppfolging_data SET veilederid = ? WHERE aktoerid = ?", veilederId.getValue(), aktorId.get());
    }

    public void settNyForVeileder(AktorId aktoerId, boolean nyForVeileder) {
        db.update("UPDATE oppfolging_data SET ny_for_veileder = ? WHERE aktoerid = ?", nyForVeileder, aktoerId.get());
    }

    public void settManuellStatus(AktorId aktoerId, boolean manuellStatus) {
        db.update("UPDATE oppfolging_data SET manuell = ? WHERE aktoerid = ?", manuellStatus, aktoerId.get());
    }

    public void settStartdato(AktorId aktoerId, ZonedDateTime startDato) {
        db.update("UPDATE oppfolging_data SET startdato = ? WHERE  aktoerid = ?", toTimestamp(startDato), aktoerId.get());
    }

    public void slettOppfolgingData(AktorId aktoerId) {
        db.update("DELETE FROM oppfolging_data WHERE aktoerid = ?", aktoerId.get());
    }

    public Optional<BrukerOppdatertInformasjon> hentOppfolgingData(AktorId aktoerId) {
        return Optional.ofNullable(queryForObjectOrNull(() ->
                db.queryForObject("SELECT * FROM oppfolging_data WHERE aktoerid = ?", this::mapToBrukerOppdatertInformasjon, aktoerId.get())
        ));
    }

    public boolean erUnderOppfolgingOgErAktivIdent(AktorId aktoerId) {
        return Optional.ofNullable(queryForObjectOrNull(() ->
                db.queryForObject("""
                        select oppfolging from oppfolging_data od
                        inner join aktive_identer ai on ai.aktorid = od.aktoerid
                        WHERE aktoerid = ?
                        """, (s, i) -> s.getBoolean(OPPFOLGING), aktoerId.get())
        )).orElse(false);
    }

    @SneakyThrows
    private BrukerOppdatertInformasjon mapToBrukerOppdatertInformasjon(ResultSet rs, int i) {
        if (rs == null || rs.getString(AKTOERID) == null) {
            return null;
        }
        return new BrukerOppdatertInformasjon()
                .setAktoerid(rs.getString(AKTOERID))
                .setNyForVeileder(rs.getBoolean(NY_FOR_VEILEDER))
                .setOppfolging(rs.getBoolean(OPPFOLGING))
                .setVeileder(rs.getString(VEILEDERID))
                .setManuell(rs.getBoolean(MANUELL))
                .setStartDato(rs.getTimestamp(STARTDATO));
    }

    public List<AktorId> hentAlleGyldigeBrukereUnderOppfolging() {
        db.setFetchSize(10_000);
        List<AktorId> alleIder = db.queryForList("""
                select aktoerid from oppfolging_data od
                 left join bruker_identer bi on bi.ident = od.aktoerid
                 where oppfolging
                 and not historisk
                """, AktorId.class);
        db.setFetchSize(-1);

        return alleIder;
    }

    public List<AktorId> hentAlleBrukereUnderOppfolging() {
        db.setFetchSize(10_000);
        List<AktorId> alleIder = db.queryForList("SELECT aktoerid FROM oppfolging_data WHERE oppfolging", AktorId.class);
        db.setFetchSize(-1);

        return alleIder;
    }

    public Optional<VeilederId> hentVeilederForBruker(AktorId aktoerId) {
        return Optional.ofNullable(
                queryForObjectOrNull(
                        () -> db.queryForObject("select veilederid from oppfolging_data where aktoerid = ?",
                                (rs, i) -> VeilederId.veilederIdOrNull(rs.getString("veilederid")), aktoerId.get())
                ));
    }

    public Map<AktorId, Optional<ZonedDateTime>> hentStartDatoForOppfolging(Set<AktorId> aktoerIder) {
        Map<AktorId, Optional<ZonedDateTime>> result = new HashMap<>();
        return db.query("select startdato, aktoerid from oppfolging_data where aktoerid = any (?::varchar[])",
                ps -> ps.setString(1, listParam(aktoerIder.stream().map(AktorId::get).toList())),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        ZonedDateTime startDatoForOppfolging = DateUtils.toZonedDateTime(rs.getTimestamp("startdato"));
                        AktorId aktoerid = AktorId.of(rs.getString("aktoerid"));
                        result.put(aktoerid, Optional.ofNullable(startDatoForOppfolging));
                    }
                    return result;
                });
    }

    private static String listParam(List<String> identer) {
        return identer.stream().collect(Collectors.joining(",", "{", "}"));
    }
}
