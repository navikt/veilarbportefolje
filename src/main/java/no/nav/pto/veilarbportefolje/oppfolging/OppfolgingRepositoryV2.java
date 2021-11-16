package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

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
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void settUnderOppfolging(AktorId aktoerId, ZonedDateTime startDato) {
        db.update("""
                        INSERT INTO OPPFOLGING_DATA (AKTOERID, OPPFOLGING, STARTDATO ) VALUES (?,?,?)
                        ON CONFLICT (AKTOERID) DO UPDATE SET OPPFOLGING = ?, STARTDATO = ?
                        """,
                aktoerId.get(),
                true, toTimestamp(startDato),
                true, toTimestamp(startDato)
        );
    }

    public void settVeileder(AktorId aktorId, VeilederId veilederId) {
        db.update("UPDATE oppfolging_data SET veilederid = ? WHERE aktoerid = ?", veilederId.getValue(), aktorId.get());
    }

    public void settNyForVeileder(AktorId aktoerId, boolean nyForVeileder) {
        db.update("UPDATE oppfolging_data SET ny_for_veileder = ? WHERE aktoerid = ?", nyForVeileder, aktoerId);
    }

    public void settManuellStatus(AktorId aktoerId, boolean manuellStatus) {
        db.update("UPDATE oppfolging_data SET manuell = ? WHERE aktoerid = ?", manuellStatus, aktoerId);
    }

    public void slettOppfolgingData(AktorId aktoerId) {
        db.update("DELETE FROM oppfolging_data WHERE aktoerid = ?", aktoerId.get());
    }

    public Optional<BrukerOppdatertInformasjon> hentOppfolgingData(AktorId aktoerId) {
        return Optional.ofNullable(queryForObjectOrNull(() ->
                db.queryForObject("SELECT * FROM oppfolging_data WHERE aktoerid = ?", this::mapToBrukerOppdatertInformasjon, aktoerId.get())
        ));
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

    public List<AktorId> hentAlleBrukereUnderOppfolging() {
        db.setFetchSize(10_000);
        List<AktorId> alleIder = db.queryForList("SELECT aktoerid FROM oppfolging_data WHERE oppfolging", AktorId.class);
        db.setFetchSize(-1);

        return alleIder;
    }
}
