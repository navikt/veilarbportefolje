package no.nav.pto.veilarbportefolje.oppfolging;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parseJaNei;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OppfolgingRepository {

    private final JdbcTemplate db;

    public boolean settUnderOppfolging(AktorId aktoerId, ZonedDateTime startDato) {
        return SqlUtils.upsert(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .set(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString())
                .set(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .set(Table.OPPFOLGING_DATA.STARTDATO, toTimestamp(startDato))
                .where(WhereClause.equals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString()))
                .execute();
    }

    public int oppdaterStartdato(AktorId aktoerId, ZonedDateTime startDato) {
        return SqlUtils.update(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .set(Table.OPPFOLGING_DATA.STARTDATO, toTimestamp(startDato))
                .whereEquals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString())
                .execute();
    }

    public int settVeileder(AktorId aktorId, VeilederId veilederId) {
        return SqlUtils.update(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .set(Table.OPPFOLGING_DATA.VEILEDERIDENT, veilederId.toString())
                .whereEquals(Table.OPPFOLGING_DATA.AKTOERID, aktorId.toString())
                .execute();
    }

    public int settNyForVeileder(AktorId aktoerId, boolean nyForVeileder) {
        return SqlUtils.update(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .set(Table.OPPFOLGING_DATA.NY_FOR_VEILEDER, safeToJaNei(nyForVeileder))
                .whereEquals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString())
                .execute();
    }

    public int settManuellStatus(AktorId aktoerId, boolean manuellStatus) {
        return SqlUtils.update(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .set(Table.OPPFOLGING_DATA.MANUELL, safeToJaNei(manuellStatus))
                .whereEquals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString())
                .execute();
    }

    public int settOppfolgingTilFalse(AktorId aktoerId) {
        return SqlUtils.update(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .set(Table.OPPFOLGING_DATA.OPPFOLGING, safeToJaNei(false))
                .whereEquals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString())
                .execute();
    }

    public Optional<ZonedDateTime> hentStartdato(AktorId aktoerId) {
        final ZonedDateTime startDato = SqlUtils
                .select(db, Table.OPPFOLGING_DATA.TABLE_NAME, rs -> toZonedDateTime(rs.getTimestamp(Table.OPPFOLGING_DATA.STARTDATO)))
                .column(Table.OPPFOLGING_DATA.STARTDATO)
                .where(WhereClause.equals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.get()))
                .execute();

        return Optional.ofNullable(startDato);
    }

    public static String safeToJaNei(Boolean aBoolean) {
        return TRUE.equals(aBoolean) ? "J" : "N";
    }

    public boolean erUnderoppfolging(AktorId aktoerId){
        Optional<BrukerOppdatertInformasjon> oppdatertInformasjon = hentOppfolgingData(aktoerId);
        return oppdatertInformasjon.map(BrukerOppdatertInformasjon::getOppfolging).orElse(false);
    }

    public Optional<BrukerOppdatertInformasjon> hentOppfolgingData(AktorId aktoerId) {
        return retrieveOppfolgingData(aktoerId).toJavaOptional();
    }

    @Deprecated
    public Try<BrukerOppdatertInformasjon> retrieveOppfolgingData(AktorId aktoerId) {
        String id = aktoerId.toString();
        return Try.of(() -> db.queryForObject(
                "SELECT * FROM OPPFOLGING_DATA WHERE AKTOERID = ?",
                new Object[]{id},
                this::mapToBrukerOppdatertInformasjon)
        ).onFailure(e -> log.info("Fant ikke oppfølgingsdata for bruker med aktoerId {}", id));
    }

    @SneakyThrows
    private BrukerOppdatertInformasjon mapToBrukerOppdatertInformasjon(ResultSet rs, int i) {
        return new BrukerOppdatertInformasjon()
                .setAktoerid(rs.getString("AKTOERID"))
                .setEndretTimestamp(rs.getTimestamp("OPPDATERT_KILDESYSTEM"))
                .setNyForVeileder(parseJaNei(rs.getString("NY_FOR_VEILEDER"), "NY_FOR_VEILEDER"))
                .setOppfolging(parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING"))
                .setVeileder(rs.getString("VEILEDERIDENT"))
                .setManuell(parseJaNei(rs.getString("MANUELL"), "MANUELL"))
                .setStartDato(rs.getTimestamp("STARTDATO"));
    }

    public void slettOppfolgingData(AktorId aktoerId) {
        SqlUtils.delete(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .where(WhereClause.equals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.get()))
                .execute();
    }

    public List<AktorId> hentAlleBrukereUnderOppfolging() {
        db.setFetchSize(10_000);

        List<AktorId> alleIder = SqlUtils
                .select(db, Table.OPPFOLGING_DATA.TABLE_NAME, rs -> AktorId.of(rs.getString(Table.OPPFOLGING_DATA.AKTOERID)))
                .column(Table.OPPFOLGING_DATA.AKTOERID)
                .where(WhereClause.equals(Table.OPPFOLGING_DATA.OPPFOLGING,"J"))
                .executeToList()
                .stream()
                .filter(Objects::nonNull)
                .collect(toList());
        db.setFetchSize(-1);

        return alleIder;
    }
}
