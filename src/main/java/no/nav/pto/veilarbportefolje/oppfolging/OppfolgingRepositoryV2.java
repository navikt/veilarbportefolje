package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
public class OppfolgingRepositoryV2 {
    private final JdbcTemplate db;

    @Autowired
    public OppfolgingRepositoryV2(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public int settUnderOppfolging(AktorId aktoerId, ZonedDateTime startDato) {
        return db.update(
                "INSERT INTO " + TABLE_NAME + " (" + AKTOERID + ", " + OPPFOLGING + ", " + STARTDATO + ") VALUES (?,?,?) " +
                        "ON CONFLICT (" + AKTOERID + ") DO UPDATE SET " + OPPFOLGING + "  = ?, " + STARTDATO + " = ?;",
                aktoerId.get(),
                true, toTimestamp(startDato),
                true, toTimestamp(startDato)
        );
    }

    public int settVeileder(AktorId aktorId, VeilederId veilederId) {
        return SqlUtils.update(db, TABLE_NAME)
                .set(VEILEDERID, veilederId.getValue())
                .set(NY_FOR_VEILEDER, true)
                .whereEquals(AKTOERID, aktorId.get())
                .execute();
    }

    public int settNyForVeileder(AktorId aktoerId, boolean nyForVeileder) {
        return SqlUtils.update(db, TABLE_NAME)
                .set(NY_FOR_VEILEDER, nyForVeileder)
                .whereEquals(AKTOERID, aktoerId.get())
                .execute();
    }

    public int settManuellStatus(AktorId aktoerId, boolean manuellStatus) {
        return SqlUtils.update(db, TABLE_NAME)
                .set(MANUELL, manuellStatus)
                .whereEquals(AKTOERID, aktoerId.get())
                .execute();
    }

    public int settOppfolgingTilFalse(AktorId aktoerId) {
        return SqlUtils.update(db, TABLE_NAME)
                .set(OPPFOLGING, false)
                .whereEquals(AKTOERID, aktoerId.get())
                .execute();
    }

    public Optional<ZonedDateTime> hentStartdato(AktorId aktoerId) {
        final ZonedDateTime startDato = SqlUtils
                .select(db, TABLE_NAME, rs -> toZonedDateTime(rs.getTimestamp(STARTDATO)))
                .column(STARTDATO)
                .where(WhereClause.equals(AKTOERID, aktoerId.get()))
                .execute();

        return Optional.ofNullable(startDato);
    }

    public void slettOppfolgingData(AktorId aktoerId) {
        SqlUtils.delete(db, TABLE_NAME)
                .where(WhereClause.equals(AKTOERID, aktoerId.get()))
                .execute();
    }

    public Optional<BrukerOppdatertInformasjon> hentOppfolgingData(AktorId aktoerId) {
        final BrukerOppdatertInformasjon oppfolging = SqlUtils.select(db, TABLE_NAME, this::mapToBrukerOppdatertInformasjon)
                .column("*")
                .where(WhereClause.equals(AKTOERID, aktoerId.get()))
                .execute();

        return Optional.ofNullable(oppfolging);
    }

    @SneakyThrows
    private BrukerOppdatertInformasjon mapToBrukerOppdatertInformasjon(ResultSet rs) {
        if(rs == null || rs.getString(AKTOERID) == null){
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

}
