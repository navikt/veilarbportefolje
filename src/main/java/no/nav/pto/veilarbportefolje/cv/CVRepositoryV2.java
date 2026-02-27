package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.Optional;
import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_CV.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CVRepositoryV2 {
    private final JdbcTemplate db;

    public int upsertCVEksisterer(AktorId aktoerId, boolean cvEksisterer) {
        return db.update("""
                        INSERT INTO bruker_cv
                        (AKTOERID, CV_EKSISTERER, SISTE_MELDING_MOTTATT) VALUES (?, ?, ?)
                        ON CONFLICT (AKTOERID) DO UPDATE SET (CV_EKSISTERER, SISTE_MELDING_MOTTATT)
                        = (excluded.cv_eksisterer, excluded.siste_melding_mottatt)
                        """,
                aktoerId.get(), cvEksisterer, Timestamp.from(now()));
    }

    public int upsertCVEksistererINyTabell(Fnr fnr,  Timestamp cvSistEndret, boolean cvEksisterer) {
        return db.update("""
                        INSERT INTO bruker_registrert_cv
                        (FNR, CV_EKSISTERER, CV_SIST_ENDRET, SISTE_MELDING_MOTTATT) VALUES (?, ?, ?, ?)
                        ON CONFLICT (FNR) DO UPDATE SET (CV_EKSISTERER, CV_SIST_ENDRET, SISTE_MELDING_MOTTATT)
                        = (excluded.cv_eksisterer, excluded.cv_sist_endret, excluded.siste_melding_mottatt)
                        """,
                fnr.get(), cvEksisterer, cvSistEndret, Timestamp.from(now()));
    }


    public boolean cvEksisterer(AktorId aktoerId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", CV_EKSISTERER, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getBoolean(CV_EKSISTERER), aktoerId.get()))
        ).orElse(false);
    }

    public void slettCvEksistererFraNyTabell(Fnr fnr) {
        secureLog.info("Sletter informasjon om at CV finnes for bruker med aktoerid {}", fnr);
        db.update(String.format("DELETE FROM %s WHERE %s = ?", NY_TABLE_NAME, FNR), fnr.get());
    }

    public void slettCvEksisterer(AktorId aktoerId) {
        secureLog.info("Sletter informasjon om at CV finnes for bruker {}", aktoerId);
        db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktoerId.get());
    }
}
