package no.nav.fo.database;

import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.where.WhereClause;

import org.springframework.jdbc.core.JdbcTemplate;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import static no.nav.fo.domene.Brukerdata.safeToJaNei;
import static no.nav.fo.util.DbUtils.parseJaNei;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

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
            .set("AKTOERID", info.getAktoerid())
            .where(WhereClause.equals("AKTOERID", info.getAktoerid()))
            .execute();
    }
    
    public Try<BrukerOppdatertInformasjon> retrieveOppfolgingData(String aktoerId) {
        return Try.of(() -> db.queryForObject(
                "SELECT * FROM VW_OPPFOLGING_DATA WHERE AKTOERID = ?", 
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
                .setVeileder(rs.getString("VEILEDERIDENT"));
    }

}
