package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.ANSVARLIG_VEILDERIDENT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.ANSVARLIG_VEILDERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.ENDRET_TIDSPUNKT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.HOVEDMAL;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.INNSATSGRUPPE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.VEDTAKID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.VEDTAKSTATUS.VEDTAKSTATUS;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VedtakStatusRepositoryV2 {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void slettGamleVedtakOgUtkast(String aktorId) {
        if (aktorId == null) {
            return;
        }
        log.info("Slettet vedtak og utkast pa bruker: {}", aktorId);
        db.update("DELETE FROM VEDTAKSTATUS WHERE AKTOERID = ?", aktorId);
    }

    public int upsertVedtak(KafkaVedtakStatusEndring vedtakStatusEndring) {
        return db.update(""" 
                        INSERT INTO VEDTAKSTATUS(
                        AKTOERID, VEDTAKID, VEDTAKSTATUS,
                        INNSATSGRUPPE, HOVEDMAL, ANSVARLIG_VEILDERIDENT,
                        ANSVARLIG_VEILDERNAVN, ENDRET_TIDSPUNKT)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (AKTOERID) DO UPDATE SET (
                        VEDTAKID, VEDTAKSTATUS, INNSATSGRUPPE, HOVEDMAL,
                        ANSVARLIG_VEILDERIDENT, ANSVARLIG_VEILDERNAVN, ENDRET_TIDSPUNKT) = (
                        excluded.VEDTAKID, excluded.VEDTAKSTATUS, excluded.INNSATSGRUPPE, excluded.HOVEDMAL,
                        excluded.ANSVARLIG_VEILDERIDENT, excluded.ANSVARLIG_VEILDERNAVN, excluded.ENDRET_TIDSPUNKT
                        )
                        """, vedtakStatusEndring.getAktorId(),
                vedtakStatusEndring.getVedtakId(), vedtakStatusEndring.getVedtakStatusEndring().name(),
                Optional.ofNullable(vedtakStatusEndring.getInnsatsgruppe()).map(Enum::name).orElse(null),
                Optional.ofNullable(vedtakStatusEndring.getHovedmal()).map(Enum::name).orElse(null),
                vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getVeilederNavn(),
                vedtakStatusEndring.getTimestamp()
        );
    }

    public int updateVedtak(KafkaVedtakStatusEndring vedtakStatusEndring) {
        if (erIkkeUtkast(vedtakStatusEndring.getAktorId(), vedtakStatusEndring.getVedtakId())) {
            log.info("Oppdaterte IKKE vedtak: {} for bruker: {}", vedtakStatusEndring.getVedtakId(), vedtakStatusEndring.getAktorId());
            return 0;
        }
        return db.update("""
                        UPDATE  VEDTAKSTATUS SET
                        (VEDTAKSTATUS, INNSATSGRUPPE, HOVEDMAL, ENDRET_TIDSPUNKT)
                        = (?, ?, ?, ?)
                        WHERE AKTOERID = ?
                        """,
                vedtakStatusEndring.getVedtakStatusEndring().name(),
                Optional.ofNullable(vedtakStatusEndring.getInnsatsgruppe()).map(Enum::name).orElse(null),
                Optional.ofNullable(vedtakStatusEndring.getHovedmal()).map(Enum::name).orElse(null),
                vedtakStatusEndring.getTimestamp(), vedtakStatusEndring.getAktorId()
        );
    }

    public Optional<KafkaVedtakStatusEndring> hentVedtak(String aktorId) {
        String sql = "SELECT * FROM VEDTAKSTATUS WHERE AKTOERID = ?";
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
        if (erIkkeUtkast(vedtakStatusEndring.getAktorId(), vedtakStatusEndring.getVedtakId())) {
            log.info("Oppdaterte IKKE vedtak: {} med ny ansvarlig veileder: {}, for bruker: {}", vedtakStatusEndring.getVedtakId(), vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getAktorId());
            return;
        }
        String sql = "UPDATE VEDTAKSTATUS SET ANSVARLIG_VEILDERIDENT = ?, ANSVARLIG_VEILDERNAVN = ? WHERE AKTOERID= ?";
        int rows = db.update(sql, vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getVeilederNavn(), vedtakStatusEndring.getAktorId());
        log.info("Oppdaterte vedtak: {} med ny ansvarlig veileder: {}, for bruker: {}. Rader: {}", vedtakStatusEndring.getVedtakId(), vedtakStatusEndring.getVeilederIdent(), vedtakStatusEndring.getAktorId(), rows);
    }

    private boolean erIkkeUtkast(String aktorId, long vedtakId) {
        return hentVedtak(aktorId).map(lagretVedtak -> lagretVedtak.getVedtakId() != vedtakId).orElse(true);
    }
}
