package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.*;

@Slf4j
@Repository
public class VedtakStatusRepositoryV2 {

    private final JdbcTemplate db;

    @Autowired
    public VedtakStatusRepositoryV2(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public void slettGamleVedtakOgUtkast(String aktorId) {
        if (aktorId == null) {
            return;
        }
        log.info("Sletter vedtak og utkast pa bruker: {}", aktorId);
        db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktorId);
    }

    public int upsertVedtak(KafkaVedtakStatusEndring vedtakStatusEndring) {
        return db.update("INSERT INTO " + TABLE_NAME +
                " (" + SQLINSERT_STRING + ") " +
                "VALUES (" + vedtakStatusEndring.toSqlInsertString() + ") " +
                "ON CONFLICT (" + AKTOERID + ") DO UPDATE SET (" + SQLUPDATE_STRING + ") = (" + vedtakStatusEndring.toSqlUpdateString() + ")");
    }

    public Optional<KafkaVedtakStatusEndring> hentVedtak(String aktorId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);

        return Optional.ofNullable(db.queryForObject(
                sql, new Object[]{aktorId},
                this::mapKafkaVedtakStatusEndring)
        );
    }

    @SneakyThrows
    private KafkaVedtakStatusEndring mapKafkaVedtakStatusEndring(ResultSet rs, int rows) {
        Optional<String> hovedmal = Optional.ofNullable(rs.getString(HOVEDMAL));
        Optional<String> innsatsgruppe = Optional.ofNullable(rs.getString(INNSATSGRUPPE));

        return new KafkaVedtakStatusEndring()
                .setVedtakId(rs.getInt(VEDTAKID))
                .setHovedmal(hovedmal.map(KafkaVedtakStatusEndring.Hovedmal::valueOf).orElse(null))
                .setInnsatsgruppe(innsatsgruppe.map(KafkaVedtakStatusEndring.Innsatsgruppe::valueOf).orElse(null))
                .setVedtakStatusEndring(KafkaVedtakStatusEndring.VedtakStatusEndring.valueOf(rs.getString(VEDTAKSTATUS)))
                .setTimestamp(rs.getTimestamp(ENDRET_TIDSPUNKT).toLocalDateTime())
                .setAktorId(rs.getString(AKTOERID))
                .setVeilederIdent(rs.getString(ANSVARLIG_VEILDERIDENT))
                .setVeilederNavn(rs.getString(ANSVARLIG_VEILDERNAVN));
    }

    public void oppdaterAnsvarligVeileder(KafkaVedtakStatusEndring vedtakStatusEndring) {
        String sql = String.format(
                "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                TABLE_NAME, ANSVARLIG_VEILDERIDENT, ANSVARLIG_VEILDERNAVN, AKTOERID
        );
        int rows = db.update(sql, vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getVeilederNavn(), vedtakStatusEndring.getAktorId());
        log.info("Oppdaterte veilder til: {} for bruker {}, rader: {}", vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getAktorId(), rows);
    }
}
