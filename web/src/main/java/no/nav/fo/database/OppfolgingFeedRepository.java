package no.nav.fo.database;

import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.PersonId;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;

import static no.nav.fo.database.BrukerRepository.BRUKERDATA;
import static no.nav.fo.domene.Brukerdata.safeToJaNei;

public class OppfolgingFeedRepository {

    private JdbcTemplate db;

    @Inject
    public OppfolgingFeedRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertVeilederOgOppfolginsinfo(BrukerOppdatertInformasjon bruker, PersonId personId) {
        SqlUtils.upsert(db, BRUKERDATA)
                .set("VEILEDERIDENT", bruker.getVeileder())
                .set("PERSONID", personId.toString())
                .set("TILDELT_TIDSPUNKT", bruker.getEndretTimestamp())
                .set("OPPFOLGING", safeToJaNei(bruker.getOppfolging()))
                .set("AKTOERID", bruker.getAktoerid())
                .where(WhereClause.equals("PERSONID", personId.toString()))
                .execute();
    }
}
