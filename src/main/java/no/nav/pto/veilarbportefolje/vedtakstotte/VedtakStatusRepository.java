package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.domene.Hovedmal;
import no.nav.pto.veilarbportefolje.domene.Innsatsgruppe;
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

    public void insertVedtak (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        Hovedmal hovedmal = kafkaVedtakStatusEndring.getHovedmal();
        Innsatsgruppe innsatsgruppe =  kafkaVedtakStatusEndring.getInnsatsgruppe();

        SqlUtils.insert(db, "VEDTAKSTATUS_DATA")
                .value("AKTOERID", kafkaVedtakStatusEndring.getAktorId())
                .value("VEDTAKSTATUS", kafkaVedtakStatusEndring.getVedtakStatus().name())
                .value("INNSATSGRUPPE", innsatsgruppe!= null ? innsatsgruppe.name(): null)
                .value("HOVEDMAL", hovedmal!=null ? hovedmal.name(): null)
                .value("VEDTAK_STATUS_ENDRET_TIDSPUNKT", Timestamp.valueOf(kafkaVedtakStatusEndring.getStatusEndretTidspunkt()))
                .value("VEDTAKID", kafkaVedtakStatusEndring.getVedtakId())
                .execute();
    }


    public void updateVedtak (KafkaVedtakStatusEndring kafkaVedtakStatusEndring) {
        Hovedmal hovedmal = kafkaVedtakStatusEndring.getHovedmal();
        Innsatsgruppe innsatsgruppe =  kafkaVedtakStatusEndring.getInnsatsgruppe();

        SqlUtils.update(db, "VEDTAKSTATUS_DATA")
                .set("AKTOERID", kafkaVedtakStatusEndring.getAktorId())
                .set("VEDTAKSTATUS", kafkaVedtakStatusEndring.getVedtakStatus().name())
                .set("INNSATSGRUPPE", innsatsgruppe!= null ? innsatsgruppe.name(): null)
                .set("HOVEDMAL", hovedmal!=null ? hovedmal.name(): null)
                .set("VEDTAK_STATUS_ENDRET_TIDSPUNKT", Timestamp.valueOf(kafkaVedtakStatusEndring.getStatusEndretTidspunkt()))
                .whereEquals("VEDTAKID", kafkaVedtakStatusEndring.getVedtakId())
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
                .setHovedmal(hovedmal!= null ? Hovedmal.valueOf(hovedmal): null)
                .setInnsatsgruppe(innsatsgruppe!= null ? Innsatsgruppe.valueOf(rs.getString("INNSATSGRUPPE")): null)
                .setVedtakStatus(KafkaVedtakStatusEndring.KafkaVedtakStatus.valueOf(rs.getString("VEDTAKSTATUS")))
                .setStatusEndretTidspunkt(rs.getTimestamp("VEDTAK_STATUS_ENDRET_TIDSPUNKT").toLocalDateTime())
                .setAktorId(rs.getString("AKTOERID"));

    }
}
