package no.nav.fo.feed;

import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

public class DialogFeedRepository {
    private JdbcTemplate db;

    public DialogFeedRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void upsertDialogdata(DialogDataFraFeed dialog, PersonId personId) {
        SqlUtils.upsert(db, "BRUKER_DATA")
                .set("VENTERPASVARFRABRUKER", toTimestamp(dialog.getTidspunktEldsteVentende()))
                .set("VENTERPASVARFRANAV", toTimestamp(dialog.getTidspunktEldsteUbehandlede()))
                .set("AKTOERID", dialog.getAktorId())
                .set("PERSONID", personId.toString())
                .where(WhereClause.equals("PERSONID", personId.toString())).execute();
    }

    private Timestamp toTimestamp(Date date) {
        if(Objects.isNull(date)) {
            return null;
        }
        return new Timestamp(date.toInstant().toEpochMilli());
    }
}