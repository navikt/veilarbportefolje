package no.nav.pto.veilarbportefolje.oppfolging;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parseJaNei;

@Slf4j
public class OppfolgingRepository {

    private JdbcTemplate db;

    @Inject
    public OppfolgingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void oppdaterOppfolgingData(BrukerOppdatertInformasjon info) {
        SqlUtils.upsert(db, "OPPFOLGING_DATA")
                .set("VEILEDERIDENT", info.getVeileder())
                .set("OPPDATERT_KILDESYSTEM", info.getEndretTimestamp())
                .set("OPPDATERT_PORTEFOLJE", Timestamp.from(Instant.now()))
                .set("OPPFOLGING", safeToJaNei(info.getOppfolging()))
                .set("NY_FOR_VEILEDER", safeToJaNei(info.getNyForVeileder()))
                .set("MANUELL", safeToJaNei(info.getManuell()))
                .set("AKTOERID", info.getAktoerid())
                .set("STARTDATO", info.getStartDato())
                .set("FEED_ID", info.getFeedId())
                .where(WhereClause.equals("AKTOERID", info.getAktoerid()))
                .execute();
    }

    public Result<AktoerId> oppdaterOppfolgingData(OppfolgingStatus dto) {
        String aktoerId = dto.getAktoerId().aktoerId;

        Supplier<Boolean> query = () -> SqlUtils.upsert(db, "OPPFOLGING_DATA")
                .set("VEILEDERIDENT", dto.getVeilederId().veilederId)
                .set("OPPDATERT_KILDESYSTEM", dto.getEndretTimestamp())
                .set("OPPDATERT_PORTEFOLJE", Timestamp.from(Instant.now()))
                .set("OPPFOLGING", safeToJaNei(dto.isOppfolging()))
                .set("NY_FOR_VEILEDER", safeToJaNei(dto.isNyForVeileder()))
                .set("MANUELL", safeToJaNei(dto.isManuell()))
                .set("AKTOERID", aktoerId)
                .set("STARTDATO", dto.getStartDato())
                .where(WhereClause.equals("AKTOERID", aktoerId))
                .execute();

        return Result.of(query).mapOk(_queryResult -> dto.getAktoerId());
    }


    public static String safeToJaNei(Boolean aBoolean) {
        return TRUE.equals(aBoolean) ? "J" : "N";
    }

    public Result<BrukerOppdatertInformasjon> hentOppfolgingData(AktoerId aktoerId) {
        Supplier<BrukerOppdatertInformasjon> query = () ->
                db.queryForObject(
                        "SELECT * FROM OPPFOLGING_DATA WHERE AKTOERID = ?",
                        new Object[]{aktoerId.aktoerId},
                        this::mapToBrukerOppdatertInformasjon
                );

        return Result.of(query);
    }


    @Deprecated
    public Try<BrukerOppdatertInformasjon> retrieveOppfolgingData(AktoerId aktoerId) {
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

    public void updateOppfolgingFeedId(BigDecimal id) {
        log.info("Oppdaterer feed_id for oppfølging: {}", id);
        SqlUtils.update(db, "METADATA").set("oppfolging_sist_oppdatert_id", id).execute();
    }


}
