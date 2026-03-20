package no.nav.pto.veilarbportefolje.tiltaksaktivitet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.postgres.utils.TiltakaktivitetEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltaksaktivitetRepository {

    private final JdbcTemplate db;

    public void upsert(TiltakaktivitetEntity tiltakaktivitet, AktorId aktorId) {
        LocalDateTime fraDato = getLocalDateTimeOrNull(tiltakaktivitet.getFraDato(), false);
        LocalDateTime tilDato = getLocalDateTimeOrNull(tiltakaktivitet.getTilDato(), true);

        secureLog.info("Lagrer tiltaksaktivitet med id {} i tabellen tiltaksaktivitet", tiltakaktivitet.getAktivitetId());

        if (!eksistererTiltakstype(tiltakaktivitet.getTiltakskode())) {
            upsertTiltakskodeverk(tiltakaktivitet.getTiltakskode(), tiltakaktivitet.getTiltaksnavn());
        }

        db.update("""
                        INSERT INTO TILTAKSAKTIVITET
                        (aktivitetid, aktoerid, tiltakskode, fradato, tildato, version, status) VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (aktivitetid) DO UPDATE SET (aktoerid, tiltakskode, fradato, tildato, version, status)
                        = (excluded.aktoerid, excluded.tiltakskode, excluded.fradato, excluded.tildato, excluded.version, excluded.status)
                        """,
                tiltakaktivitet.getAktivitetId(), aktorId.get(), tiltakaktivitet.getTiltakskode(), fraDato, tilDato, tiltakaktivitet.getVersion(), tiltakaktivitet.getStatus()
        );
    }

    public void deleteTiltaksaktivitet(String tiltaksaktivitetId) {
        secureLog.info("Sletter tiltaksaktivitet med id {} fra tabellen tiltaksaktivitet", tiltaksaktivitetId);
        db.update(
                String.format("DELETE FROM %s WHERE %s = ?", PostgresTable.TILTAKSAKTIVITET.TABLE_NAME, PostgresTable.TILTAKSAKTIVITET.AKTIVITETID),
                tiltaksaktivitetId
        );
    }

    public boolean eksistererTiltakstype(String tiltakskode) {
        String sql = String.format("SELECT EXISTS (SELECT 1 FROM %s WHERE %s = ? LIMIT 1)", PostgresTable.TILTAKSKODEVERK.TABLE_NAME, PostgresTable.TILTAKSKODEVERK.KODE);
        return db.queryForObject(sql, boolean.class, tiltakskode);
    }

    public void upsertTiltakskodeverk(String tiltakskode, String tiltaksnavn) {
        String sql = """
                        INSERT INTO tiltakskodeverk (kode, verdi) VALUES (?, ?)
                        ON CONFLICT (kode) DO UPDATE SET verdi = excluded.verdi
                        """;
        db.update(sql, tiltakskode, tiltaksnavn);
    }

    public Long hentSistVersjonAvAktivitet(String aktivitetId) {
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(
                        "SELECT * FROM TILTAKSAKTIVITET WHERE aktivitetid = ?",
                        (rs, row) -> rs.getLong("version"),
                        aktivitetId
                ))
        ).orElse(-1L);
    }
}
