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

import static no.nav.pto.veilarbportefolje.database.Table.VEDTAK;

@Repository
public class VedtakStatusRepository {

    private final JdbcTemplate db;

    @Autowired
    public VedtakStatusRepository(JdbcTemplate db) { this.db = db; }

    public void slettVedtakUtkast (long id) {
        SqlUtils.delete(db, VEDTAK.TABLE_NAME)
                .where(WhereClause.equals(VEDTAK.VEDTAKID, id))
                .execute();
    }

    public void slettGamleVedtakOgUtkast (String aktorId) {
        SqlUtils.delete(db, VEDTAK.TABLE_NAME)
                .where(WhereClause.equals(VEDTAK.AKTOERID, aktorId))
                .execute();
    }

    public void upsertVedtak (KafkaVedtakStatusEndring vedtakStatusEndring) {
        Optional<KafkaVedtakStatusEndring.Hovedmal> hovedmal = Optional.ofNullable(vedtakStatusEndring.getHovedmal());
        Optional<KafkaVedtakStatusEndring.Innsatsgruppe> innsatsgruppe =  Optional.ofNullable(vedtakStatusEndring.getInnsatsgruppe());

        SqlUtils.upsert(db, VEDTAK.TABLE_NAME)
                .set(VEDTAK.AKTOERID, vedtakStatusEndring.getAktorId())
                .set(VEDTAK.VEDTAKSTATUS, vedtakStatusEndring.getVedtakStatusEndring().name())
                .set(VEDTAK.INNSATSGRUPPE, innsatsgruppe.map(Enum::name).orElse(null))
                .set(VEDTAK.HOVEDMAL, hovedmal.map(Enum::name).orElse(null))
                .set(VEDTAK.VEDTAK_STATUS_ENDRET_TIDSPUNKT, Timestamp.valueOf(vedtakStatusEndring.getTimestamp()))
                .set(VEDTAK.VEDTAKID, vedtakStatusEndring.getVedtakId())
                .where(WhereClause.equals(VEDTAK.VEDTAKID, vedtakStatusEndring.getVedtakId()))
                .execute();
    }

    public void opprettUtkast (KafkaVedtakStatusEndring vedtakStatusEndring) {
        Optional<KafkaVedtakStatusEndring.Hovedmal> hovedmal = Optional.ofNullable(vedtakStatusEndring.getHovedmal());
        Optional<KafkaVedtakStatusEndring.Innsatsgruppe> innsatsgruppe =  Optional.ofNullable(vedtakStatusEndring.getInnsatsgruppe());

        SqlUtils.upsert(db, VEDTAK.TABLE_NAME)
                .set(VEDTAK.AKTOERID, vedtakStatusEndring.getAktorId())
                .set(VEDTAK.VEDTAKSTATUS, vedtakStatusEndring.getVedtakStatusEndring().name())
                .set(VEDTAK.INNSATSGRUPPE, innsatsgruppe.map(Enum::name).orElse(null))
                .set(VEDTAK.HOVEDMAL, hovedmal.map(Enum::name).orElse(null))
                .set(VEDTAK.VEDTAK_STATUS_ENDRET_TIDSPUNKT, Timestamp.valueOf(vedtakStatusEndring.getTimestamp()))
                .set(VEDTAK.VEDTAKID, vedtakStatusEndring.getVedtakId())
                .set(VEDTAK.ANSVARLIG_VEILEDER_IDENT, vedtakStatusEndring.getVeilederIdent())
                .set(VEDTAK.ANSVARLIG_VEILEDER_NAVN, vedtakStatusEndring.getVeilederNavn())
                .where(WhereClause.equals(VEDTAK.VEDTAKID, vedtakStatusEndring.getVedtakId()))
                .execute();
    }


    public List<KafkaVedtakStatusEndring> hentVedtak (String aktorId) {
        return SqlUtils.select(db, VEDTAK.TABLE_NAME, VedtakStatusRepository::mapKafkaVedtakStatusEndring)
                .where(WhereClause.equals(VEDTAK.AKTOERID, aktorId))
                .column("*")
                .executeToList();
    }

    @SneakyThrows
    private static KafkaVedtakStatusEndring mapKafkaVedtakStatusEndring(ResultSet rs){
        Optional<String> hovedmal = Optional.ofNullable(rs.getString(VEDTAK.HOVEDMAL));
        Optional<String> innsatsgruppe =  Optional.ofNullable(rs.getString(VEDTAK.INNSATSGRUPPE));
        return new KafkaVedtakStatusEndring()
                .setVedtakId(rs.getInt(VEDTAK.VEDTAKID))
                .setHovedmal(hovedmal.map(KafkaVedtakStatusEndring.Hovedmal::valueOf).orElse(null))
                .setInnsatsgruppe(innsatsgruppe.map(KafkaVedtakStatusEndring.Innsatsgruppe::valueOf).orElse( null))
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.valueOf(rs.getString(VEDTAK.VEDTAKSTATUS)))
                .setTimestamp(rs.getTimestamp(VEDTAK.VEDTAK_STATUS_ENDRET_TIDSPUNKT).toLocalDateTime())
                .setAktorId(rs.getString(VEDTAK.AKTOERID))
                .setVeilederIdent(rs.getString(VEDTAK.ANSVARLIG_VEILEDER_IDENT))
                .setVeilederNavn(rs.getString(VEDTAK.ANSVARLIG_VEILEDER_NAVN));
    }

    public void oppdaterAnsvarligVeileder(KafkaVedtakStatusEndring vedtakStatusEndring) {
        SqlUtils.update(db, VEDTAK.TABLE_NAME)
                .set(VEDTAK.ANSVARLIG_VEILEDER_IDENT, vedtakStatusEndring.getVeilederIdent())
                .set(VEDTAK.ANSVARLIG_VEILEDER_NAVN, vedtakStatusEndring.getVeilederNavn())
                .whereEquals(VEDTAK.VEDTAKID, vedtakStatusEndring.getVedtakId())
                .execute();
    }
}
