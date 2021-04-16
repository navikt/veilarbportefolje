package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGING_DATA;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parseJaNei;

@Slf4j
public class OppfolgingRepositoryV2 {

    private final JdbcTemplate db;

    @Autowired
    public OppfolgingRepositoryV2(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public boolean settUnderOppfolging(AktorId aktoerId, ZonedDateTime startDato) {
        return SqlUtils.upsert(db, OPPFOLGING_DATA.TABLE_NAME)
                .set(OPPFOLGING_DATA.AKTOERID, aktoerId.get())
                .set(OPPFOLGING_DATA.OPPFOLGING, true)
                .set(OPPFOLGING_DATA.STARTDATO, toTimestamp(startDato))
                .where(WhereClause.equals(OPPFOLGING_DATA.AKTOERID, aktoerId.get()))
                .execute();
    }

    public int settVeileder(AktorId aktorId, VeilederId veilederId) {
        return SqlUtils.update(db, OPPFOLGING_DATA.TABLE_NAME)
                .set(OPPFOLGING_DATA.VEILEDERID, veilederId.getValue())
                .set(OPPFOLGING_DATA.NY_FOR_VEILEDER, true)
                .whereEquals(OPPFOLGING_DATA.AKTOERID, aktorId.get())
                .execute();
    }

    public int settNyForVeileder(AktorId aktoerId, boolean nyForVeileder) {
        return SqlUtils.update(db, OPPFOLGING_DATA.TABLE_NAME)
                .set(OPPFOLGING_DATA.NY_FOR_VEILEDER, nyForVeileder)
                .whereEquals(OPPFOLGING_DATA.AKTOERID, aktoerId.get())
                .execute();
    }

    public int settManuellStatus(AktorId aktoerId, boolean manuellStatus) {
        return SqlUtils.update(db, OPPFOLGING_DATA.TABLE_NAME)
                .set(OPPFOLGING_DATA.MANUELL, manuellStatus)
                .whereEquals(OPPFOLGING_DATA.AKTOERID, aktoerId.get())
                .execute();
    }

    public int settOppfolgingTilFalse(AktorId aktoerId) {
        return SqlUtils.update(db, OPPFOLGING_DATA.TABLE_NAME)
                .set(OPPFOLGING_DATA.OPPFOLGING, false)
                .whereEquals(OPPFOLGING_DATA.AKTOERID, aktoerId.get())
                .execute();
    }

    public Optional<ZonedDateTime> hentStartdato(AktorId aktoerId) {
        final ZonedDateTime startDato = SqlUtils
                .select(db, OPPFOLGING_DATA.TABLE_NAME, rs -> toZonedDateTime(rs.getTimestamp(OPPFOLGING_DATA.STARTDATO)))
                .column(OPPFOLGING_DATA.STARTDATO)
                .where(WhereClause.equals(OPPFOLGING_DATA.AKTOERID, aktoerId.get()))
                .execute();

        return Optional.ofNullable(startDato);
    }

    public void slettOppfolgingData(AktorId aktoerId) {
        SqlUtils.delete(db, OPPFOLGING_DATA.TABLE_NAME)
                .where(WhereClause.equals(OPPFOLGING_DATA.AKTOERID, aktoerId.get()))
                .execute();
    }

    public Optional<BrukerOppdatertInformasjon> hentOppfolgingData(AktorId aktoerId) {
        final BrukerOppdatertInformasjon oppfolging = SqlUtils.select(db, OPPFOLGING_DATA.TABLE_NAME, rs -> mapToBrukerOppdatertInformasjon(rs))
                .column("*")
                .where(WhereClause.equals(OPPFOLGING_DATA.AKTOERID, aktoerId.toString()))
                .execute();

        return Optional.ofNullable(oppfolging);
    }

    @SneakyThrows
    private BrukerOppdatertInformasjon mapToBrukerOppdatertInformasjon(ResultSet rs) {
        return new BrukerOppdatertInformasjon()
                .setAktoerid(rs.getString(OPPFOLGING_DATA.AKTOERID))
                .setNyForVeileder(rs.getBoolean(OPPFOLGING_DATA.NY_FOR_VEILEDER))
                .setOppfolging(rs.getBoolean(OPPFOLGING_DATA.OPPFOLGING))
                .setVeileder(rs.getString(OPPFOLGING_DATA.VEILEDERID))
                .setManuell(rs.getBoolean(OPPFOLGING_DATA.MANUELL))
                .setStartDato(rs.getTimestamp(OPPFOLGING_DATA.STARTDATO));
    }

}
