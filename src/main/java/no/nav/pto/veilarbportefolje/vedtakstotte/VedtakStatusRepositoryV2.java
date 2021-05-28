package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

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
                        " (" + AKTOERID +
                        ", " + VEDTAKID +
                        ", " + VEDTAKSTATUS +
                        ", " + INNSATSGRUPPE +
                        ", " + HOVEDMAL +
                        ", " + ANSVARLIG_VEILDERIDENT +
                        ", " + ANSVARLIG_VEILDERNAVN +
                        ", " + ENDRET_TIDSPUNKT + ") " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (" + AKTOERID + ") DO UPDATE SET" +
                        " (" + VEDTAKID +
                        ", " + VEDTAKSTATUS +
                        ", " + INNSATSGRUPPE +
                        ", " + HOVEDMAL +
                        ", " + ANSVARLIG_VEILDERIDENT +
                        ", " + ANSVARLIG_VEILDERNAVN +
                        ", " + ENDRET_TIDSPUNKT + ") = (?, ?, ?, ?, ?, ?, ?)", vedtakStatusEndring.getAktorId(),
                vedtakStatusEndring.getVedtakId(), vedtakStatusEndring.getVedtakStatusEndring().name(),
                Optional.ofNullable(vedtakStatusEndring.getInnsatsgruppe()).map(Enum::name).orElse(null),
                Optional.ofNullable(vedtakStatusEndring.getHovedmal()).map(Enum::name).orElse(null),
                vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getVeilederNavn(),
                vedtakStatusEndring.getTimestamp(),

                vedtakStatusEndring.getVedtakId(), vedtakStatusEndring.getVedtakStatusEndring().name(),
                Optional.ofNullable(vedtakStatusEndring.getInnsatsgruppe()).map(Enum::name).orElse(null),
                Optional.ofNullable(vedtakStatusEndring.getHovedmal()).map(Enum::name).orElse(null),
                vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getVeilederNavn(),
                vedtakStatusEndring.getTimestamp()
        );
    }

    public int updateVedtak(KafkaVedtakStatusEndring vedtakStatusEndring) {
        if (erIkkeLagretUtkast(vedtakStatusEndring.getAktorId(), vedtakStatusEndring.getVedtakId())) {
            log.info("Oppdaterte ikke vedtak pa bruker {}, gjelder vedtak: {}", vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getAktorId());
            return 0;
        }
        return db.update("UPDATE " + TABLE_NAME + " SET (" + VEDTAKSTATUS +
                        ", " + INNSATSGRUPPE +
                        ", " + HOVEDMAL +
                        ", " + ENDRET_TIDSPUNKT + ") = (?, ?, ?, ?) WHERE " + AKTOERID+" = ?",
                vedtakStatusEndring.getVedtakStatusEndring().name(),
                Optional.ofNullable(vedtakStatusEndring.getInnsatsgruppe()).map(Enum::name).orElse(null),
                Optional.ofNullable(vedtakStatusEndring.getHovedmal()).map(Enum::name).orElse(null),
                vedtakStatusEndring.getTimestamp(), vedtakStatusEndring.getAktorId()
        );
    }

    public Optional<KafkaVedtakStatusEndring> hentVedtak(String aktorId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapKafkaVedtakStatusEndring, aktorId))
        );
    }

    @SneakyThrows
    private KafkaVedtakStatusEndring mapKafkaVedtakStatusEndring(ResultSet rs, int rows) {
        if (rs == null || rs.getString(AKTOERID) == null) {
            return null;
        }
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
        if (erIkkeLagretUtkast(vedtakStatusEndring.getAktorId(), vedtakStatusEndring.getVedtakId())) {
            log.info("Oppdaterte ikke vedtak pa bruker {}, gjelder vedtak: {}", vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getAktorId());
            return;
        }
        String sql = String.format(
                "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                TABLE_NAME, ANSVARLIG_VEILDERIDENT, ANSVARLIG_VEILDERNAVN, AKTOERID
        );
        int rows = db.update(sql, vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getVeilederNavn(), vedtakStatusEndring.getAktorId());
        log.info("Oppdaterte veilder til: {} for bruker {}, rader: {}", vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getAktorId(), rows);
    }

    private boolean erIkkeLagretUtkast(String aktorId, long vedtakId) {
        return hentVedtak(aktorId).map(lagretVedtak -> lagretVedtak.getVedtakId() != vedtakId).orElse(true);
    }
}
