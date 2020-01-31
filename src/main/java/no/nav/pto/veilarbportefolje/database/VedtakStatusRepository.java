package no.nav.pto.veilarbportefolje.database;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.Hovedmal;
import no.nav.pto.veilarbportefolje.domene.Innsatsgruppe;
import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.util.List;

public class VedtakStatusRepository {

    private JdbcTemplate db;


    public VedtakStatusRepository(JdbcTemplate db) {
        this.db = db;
    }


    public void slettVedtakUtkast (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        SqlUtils.delete(db, "VEDTAKSTATUS_DATA")
                .where(WhereClause.equals("AKTOERID", kafkaVedtakStatusEndring.getAktorId()).and(WhereClause.equals("ID", kafkaVedtakStatusEndring.getId())))
                .execute();
    }

    public void slettVedtak (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        SqlUtils.delete(db, "VEDTAKSTATUS_DATA")
                .where(WhereClause.equals("AKTOERID", kafkaVedtakStatusEndring.getAktorId()))
                .execute();
    }

    public void upsertVedtak (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        SqlUtils.upsert(db, "VEDTAKSTATUS_DATA")
                .set("AKTOERID", kafkaVedtakStatusEndring.getAktorId())
                .set("STATUS", kafkaVedtakStatusEndring.getVedtakStatus())
                .set("INNSATSGRUPPE", kafkaVedtakStatusEndring.getInnsatsgruppe())
                .set("HOVEDMAL", kafkaVedtakStatusEndring.getHovedmal())
                .set("SIST_REDIGERT_TIDSPUNKT", kafkaVedtakStatusEndring.getSistRedigertTidspunkt())
                .set("STATUS_ENDRET_TIDSPUNKT", kafkaVedtakStatusEndring.getStatusEndretTidspunkt())
                .where(WhereClause.equals("AKTOERID", kafkaVedtakStatusEndring.getAktorId()).and(WhereClause.equals("ID", kafkaVedtakStatusEndring.getId())))
                .execute();
    }

    public List<KafkaVedtakStatusEndring> hentVedtak (String aktorId) {
        return SqlUtils.select(db, "VEDTAKSTATUS_DATA", VedtakStatusRepository::mapKafkaVedtakStatusEndring)
                .where(WhereClause.equals("AKTOERID", aktorId))
                .column("*")
                .executeToList();
    }

    @SneakyThrows
    private static KafkaVedtakStatusEndring mapKafkaVedtakStatusEndring(ResultSet rs){
        return new KafkaVedtakStatusEndring()
                .setId(rs.getInt("ID"))
                .setHovedmal(Hovedmal.valueOf(rs.getString("HOVEDMAL")))
                .setInnsatsgruppe(Innsatsgruppe.valueOf(rs.getString("INNSATSGRUPPE")))
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.valueOf(rs.getString("STATUS")))
                .setSistRedigertTidspunkt(rs.getTimestamp("SIST_REDIGERT_TIDSPUNKT").toLocalDateTime())
                .setStatusEndretTidspunkt(rs.getTimestamp("STATUS_ENDRET_TIDSPUNKT").toLocalDateTime())
                .setAktorId(rs.getString("AKTOERID"));

    }
}
