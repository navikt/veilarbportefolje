package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.UTKAST_14A_STATUS.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.UTKAST_14A_STATUS.ANSVARLIG_VEILDERIDENT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.UTKAST_14A_STATUS.ANSVARLIG_VEILDERNAVN;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.UTKAST_14A_STATUS.ENDRET_TIDSPUNKT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.UTKAST_14A_STATUS.HOVEDMAL;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.UTKAST_14A_STATUS.INNSATSGRUPPE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.UTKAST_14A_STATUS.ID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.UTKAST_14A_STATUS.STATUS;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Utkast14aStatusRepository {
    private final JdbcTemplate db;

    public void slettUtkastForBruker(String aktorId) {
        if (aktorId == null) {
            return;
        }
        log.info("Slettet vedtak og utkast pa bruker: {}", aktorId);
        db.update("DELETE FROM UTKAST_14A_STATUS WHERE AKTOERID = ?", aktorId);
    }

    public int upsert(Kafka14aStatusendring statusEndring) {
        return db.update(""" 
                        INSERT INTO UTKAST_14A_STATUS(
                        AKTOERID, VEDTAKID, VEDTAKSTATUS,
                        INNSATSGRUPPE, HOVEDMAL, ANSVARLIG_VEILDERIDENT,
                        ANSVARLIG_VEILDERNAVN, ENDRET_TIDSPUNKT)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (AKTOERID) DO UPDATE SET (
                        VEDTAKID, VEDTAKSTATUS, INNSATSGRUPPE, HOVEDMAL,
                        ANSVARLIG_VEILDERIDENT, ANSVARLIG_VEILDERNAVN, ENDRET_TIDSPUNKT) = (
                        excluded.VEDTAKID, excluded.VEDTAKSTATUS, excluded.INNSATSGRUPPE, excluded.HOVEDMAL,
                        excluded.ANSVARLIG_VEILDERIDENT, excluded.ANSVARLIG_VEILDERNAVN, excluded.ENDRET_TIDSPUNKT
                        )
                        """, statusEndring.getAktorId(),
                statusEndring.getVedtakId(), statusEndring.getVedtakStatusEndring().name(),
                Optional.ofNullable(statusEndring.getInnsatsgruppe()).map(Enum::name).orElse(null),
                Optional.ofNullable(statusEndring.getHovedmal()).map(Enum::name).orElse(null),
                statusEndring.getVeilederIdent(), statusEndring.getVeilederNavn(),
                statusEndring.getTimestamp()
        );
    }

    public int update(Kafka14aStatusendring statusEndring) {
        if (erIkkeUtkast(statusEndring.getAktorId(), statusEndring.getVedtakId())) {
            log.info("Oppdaterte IKKE vedtak: {} for bruker: {}", statusEndring.getVedtakId(), statusEndring.getAktorId());
            return 0;
        }
        return db.update("""
                        UPDATE UTKAST_14A_STATUS SET
                        (VEDTAKSTATUS, INNSATSGRUPPE, HOVEDMAL, ENDRET_TIDSPUNKT)
                        = (?, ?, ?, ?)
                        WHERE AKTOERID = ?
                        """,
                statusEndring.getVedtakStatusEndring().name(),
                Optional.ofNullable(statusEndring.getInnsatsgruppe()).map(Enum::name).orElse(null),
                Optional.ofNullable(statusEndring.getHovedmal()).map(Enum::name).orElse(null),
                statusEndring.getTimestamp(), statusEndring.getAktorId()
        );
    }

    public Optional<Kafka14aStatusendring> hentStatusEndringForBruker(String aktorId) {
        String sql = "SELECT * FROM UTKAST_14A_STATUS WHERE AKTOERID = ?";
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapKafkaVedtakStatusEndring, aktorId))
        );
    }

    @SneakyThrows
    private Kafka14aStatusendring mapKafkaVedtakStatusEndring(ResultSet rs, int rows) {
        if (rs == null || rs.getString(AKTOERID) == null) {
            return null;
        }
        Optional<String> hovedmal = Optional.ofNullable(rs.getString(HOVEDMAL));
        Optional<String> innsatsgruppe = Optional.ofNullable(rs.getString(INNSATSGRUPPE));

        return new Kafka14aStatusendring()
                .setVedtakId(rs.getInt(ID))
                .setHovedmal(hovedmal.map(Kafka14aStatusendring.Hovedmal::valueOf).orElse(null))
                .setInnsatsgruppe(innsatsgruppe.map(Kafka14aStatusendring.Innsatsgruppe::valueOf).orElse(null))
                .setVedtakStatusEndring(Kafka14aStatusendring.Status.valueOf(rs.getString(STATUS)))
                .setTimestamp(rs.getTimestamp(ENDRET_TIDSPUNKT).toLocalDateTime())
                .setAktorId(rs.getString(AKTOERID))
                .setVeilederIdent(rs.getString(ANSVARLIG_VEILDERIDENT))
                .setVeilederNavn(rs.getString(ANSVARLIG_VEILDERNAVN));
    }

    public void oppdaterAnsvarligVeileder(Kafka14aStatusendring statusEndring) {
        if (erIkkeUtkast(statusEndring.getAktorId(), statusEndring.getVedtakId())) {
            log.info("Oppdaterte IKKE vedtak: {} med ny ansvarlig veileder: {}, for bruker: {}", statusEndring.getVedtakId(), statusEndring.getVeilederIdent(), statusEndring.getAktorId());
            return;
        }
        String sql = "UPDATE UTKAST_14A_STATUS SET ANSVARLIG_VEILDERIDENT = ?, ANSVARLIG_VEILDERNAVN = ? WHERE AKTOERID = ?";
        int rows = db.update(sql, statusEndring.getVeilederIdent(), statusEndring.getVeilederNavn(), statusEndring.getAktorId());
        log.info("Oppdaterte vedtak: {} med ny ansvarlig veileder: {}, for bruker: {}. Rader: {}", statusEndring.getVedtakId(), statusEndring.getVeilederIdent(), statusEndring.getAktorId(), rows);
    }

    private boolean erIkkeUtkast(String aktorId, long vedtakId) {
        return hentStatusEndringForBruker(aktorId).map(lagretVedtak -> lagretVedtak.getVedtakId() != vedtakId).orElse(true);
    }
}
