package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.SneakyThrows;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class VedtakStatusRepository {

    private final JdbcTemplate db;

    @Autowired
    public VedtakStatusRepository(JdbcTemplate db) { this.db = db; }

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

    public void upsertVedtak (KafkaVedtakStatusEndring vedtakStatusEndring) {
        Optional<KafkaVedtakStatusEndring.Hovedmal> hovedmal = Optional.ofNullable(vedtakStatusEndring.getHovedmal());
        Optional<KafkaVedtakStatusEndring.Innsatsgruppe> innsatsgruppe =  Optional.ofNullable(vedtakStatusEndring.getInnsatsgruppe());

        SqlUtils.upsert(db, "VEDTAKSTATUS_DATA")
                .set("AKTOERID", vedtakStatusEndring.getAktorId())
                .set("VEDTAKSTATUS", vedtakStatusEndring.getVedtakStatusEndring().name())
                .set("INNSATSGRUPPE", innsatsgruppe.map(Enum::name).orElse(null))
                .set("HOVEDMAL", hovedmal.map(Enum::name).orElse(null))
                .set("VEDTAK_STATUS_ENDRET_TIDSPUNKT", Timestamp.valueOf(vedtakStatusEndring.getTimestamp()))
                .set("VEDTAKID", vedtakStatusEndring.getVedtakId())
                .where(WhereClause.equals("VEDTAKID", vedtakStatusEndring.getVedtakId()))
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
        Optional<String> hovedmal = Optional.ofNullable(rs.getString("HOVEDMAL"));
        Optional<String> innsatsgruppe =  Optional.ofNullable(rs.getString("INNSATSGRUPPE"));
        return new KafkaVedtakStatusEndring()
                .setVedtakId(rs.getInt("VEDTAKID"))
                .setHovedmal(hovedmal.map(KafkaVedtakStatusEndring.Hovedmal::valueOf).orElse(null))
                .setInnsatsgruppe(innsatsgruppe.map(KafkaVedtakStatusEndring.Innsatsgruppe::valueOf).orElse( null))
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.valueOf(rs.getString("VEDTAKSTATUS")))
                .setTimestamp(rs.getTimestamp("VEDTAK_STATUS_ENDRET_TIDSPUNKT").toLocalDateTime())
                .setAktorId(rs.getString("AKTOERID"));
    }
}
