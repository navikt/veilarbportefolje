package no.nav.pto.veilarbportefolje.database;

import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.sbl.sql.SqlUtils.upsert;

public class VedtakStatusRepository {

    private JdbcTemplate db;

    public VedtakStatusRepository(JdbcTemplate ds) {
        this.db = ds;
    }

    public void upsertVedtak (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        upsert(db, "VEDTAKSTATUS")
                .set("AKTOERID", kafkaVedtakStatusEndring.getAktorId())
                .set("STATUS", kafkaVedtakStatusEndring.getVedtakStatus())
                .set("INNSATSGRUPPE", kafkaVedtakStatusEndring.getInnsatsgruppe())
                .set("HOVEDMAL", kafkaVedtakStatusEndring.getHovedmal())
                .set("SIST_REDIGERT_TIDSPUNKT", kafkaVedtakStatusEndring.getSistRedigertTidspunkt())
                .set("STATUS_ENDRET_TIDSPUNKT", kafkaVedtakStatusEndring.getStatusEndretTidspunkt())
                .where(WhereClause.equals("AKTOERID", kafkaVedtakStatusEndring.getAktorId()))
                .execute();
    }
}
