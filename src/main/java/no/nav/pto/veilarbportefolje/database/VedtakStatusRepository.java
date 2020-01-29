package no.nav.pto.veilarbportefolje.database;

import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;

import static no.nav.sbl.sql.SqlUtils.delete;
import static no.nav.sbl.sql.SqlUtils.upsert;

public class VedtakStatusRepository {

    @Inject
    private JdbcTemplate db;


    public void slettVedtakUtkast (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        delete(db, "VEDTAKSTATUS")
                .where(WhereClause.equals("AKTOERID", kafkaVedtakStatusEndring.getAktorId()).and(WhereClause.equals("ID", kafkaVedtakStatusEndring.getId())))
                .execute();
    }

    public void slettVedtak (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        delete(db, "VEDTAKSTATUS")
                .where(WhereClause.equals("AKTOERID", kafkaVedtakStatusEndring.getAktorId()))
                .execute();
    }

    public void upsertVedtak (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        upsert(db, "VEDTAKSTATUS")
                .set("AKTOERID", kafkaVedtakStatusEndring.getAktorId())
                .set("STATUS", kafkaVedtakStatusEndring.getVedtakStatus())
                .set("INNSATSGRUPPE", kafkaVedtakStatusEndring.getInnsatsgruppe())
                .set("HOVEDMAL", kafkaVedtakStatusEndring.getHovedmal())
                .set("SIST_REDIGERT_TIDSPUNKT", kafkaVedtakStatusEndring.getSistRedigertTidspunkt())
                .set("STATUS_ENDRET_TIDSPUNKT", kafkaVedtakStatusEndring.getStatusEndretTidspunkt())
                .where(WhereClause.equals("AKTOERID", kafkaVedtakStatusEndring.getAktorId()).and(WhereClause.equals("ID", kafkaVedtakStatusEndring.getId())))
                .execute();
    }
}
