package no.nav.fo.veilarbportefolje.database;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

import static java.lang.Boolean.TRUE;
import static no.nav.fo.veilarbportefolje.util.DbUtils.parseJaNei;

@Slf4j
public class OppfolgingFeedRepository {

    private JdbcTemplate db;

    @Inject
    public OppfolgingFeedRepository(JdbcTemplate db) {
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

    static String safeToJaNei(Boolean aBoolean) {
        return TRUE.equals(aBoolean) ? "J" : "N";
    }

    public Try<BrukerOppdatertInformasjon> retrieveOppfolgingData(String aktoerId) {
        return Try.of(() -> db.queryForObject(
                "SELECT * FROM OPPFOLGING_DATA WHERE AKTOERID = ?",
                new Object[] {aktoerId},
                this::mapToBrukerOppdatertInformasjon)
        ).onFailure(e -> log.info("Fant ikke oppfølgingsdata for bruker med aktoerId {}", aktoerId));
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
