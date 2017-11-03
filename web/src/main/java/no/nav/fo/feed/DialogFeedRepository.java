package no.nav.fo.feed;

import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.fo.database.BrukerRepository.BRUKERDATA;

public class DialogFeedRepository {
    private JdbcTemplate db;

    public DialogFeedRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void upsertDialogdata(DialogDataFraFeed dialog, PersonId personId) {
        SqlUtils.upsert(db, BRUKERDATA)
                .set("VENTERPASVARFRABRUKER", dialog.getTidspunktEldsteVentende())
                .set("VENTERPASVARFRANAV", dialog.getTidspunktEldsteUbehandlede())
                .set("AKTOERID", dialog.getAktorId())
                .set("PERSONID", personId.toString())
                .where(WhereClause.equals("PERSONID", personId.toString())).execute();
    }
}