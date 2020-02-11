package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.SneakyThrows;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

public class VedtakStatusRepository {

    private JdbcTemplate db;

    public VedtakStatusRepository(JdbcTemplate db) {
        this.db = db;
    }


    public void slettVedtakUtkast (long id) {
        SqlUtils.delete(db, "VEDTAKSTATUS_DATA")
                .where(WhereClause.equals("VEDTAKID", id))
                .execute();
    }

    public void slettGamleVedtakOgUtkast (String aktorId) {
        SqlUtils.delete(db, "VEDTAKSTATUS_DATA")
                .where(WhereClause.equals("AKTOERID", aktorId))
                .execute();
    }

    public void upsertVedtak (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        KafkaVedtakStatusEndring.Hovedmal hovedmal = kafkaVedtakStatusEndring.getHovedmal();
        KafkaVedtakStatusEndring.Innsatsgruppe innsatsgruppe =  kafkaVedtakStatusEndring.getInnsatsgruppe();

        SqlUtils.upsert(db, "VEDTAKSTATUS_DATA")
                .set("AKTOERID", kafkaVedtakStatusEndring.getAktorId())
                .set("VEDTAKSTATUS", kafkaVedtakStatusEndring.getVedtakStatus().name())
                .set("INNSATSGRUPPE", innsatsgruppe!= null ? innsatsgruppe.name(): null)
                .set("HOVEDMAL", hovedmal!=null ? hovedmal.name(): null)
                .set("VEDTAK_STATUS_ENDRET_TIDSPUNKT", Timestamp.valueOf(kafkaVedtakStatusEndring.getStatusEndretTidspunkt()))
                .set("VEDTAKID", kafkaVedtakStatusEndring.getVedtakId())
                .where(WhereClause.equals("VEDTAKID", kafkaVedtakStatusEndring.getVedtakId()))
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
        String hovedmal =  rs.getString("HOVEDMAL");
        String innsatsgruppe =  rs.getString("INNSATSGRUPPE");
        return new KafkaVedtakStatusEndring()
                .setVedtakId(rs.getInt("VEDTAKID"))
                .setHovedmal(hovedmal!= null ? KafkaVedtakStatusEndring.Hovedmal.valueOf(hovedmal): null)
                .setInnsatsgruppe(innsatsgruppe!= null ? KafkaVedtakStatusEndring.Innsatsgruppe.valueOf(rs.getString("INNSATSGRUPPE")): null)
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.valueOf(rs.getString("VEDTAKSTATUS")))
                .setStatusEndretTidspunkt(rs.getTimestamp("VEDTAK_STATUS_ENDRET_TIDSPUNKT").toLocalDateTime())
                .setAktorId(rs.getString("AKTOERID"));
    }
}
