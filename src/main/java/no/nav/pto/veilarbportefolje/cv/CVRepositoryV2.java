package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_REGISTRERT_CV.*;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CVRepositoryV2 {
    private final JdbcTemplate db;

    public int upsertCvRegistrert(Fnr fnr, Timestamp cvSistEndret, boolean cvEksisterer) {
        return db.update("""
                        INSERT INTO bruker_registrert_cv
                        (FNR, CV_EKSISTERER, CV_SIST_ENDRET, SISTE_MELDING_MOTTATT) VALUES (?, ?, ?, ?)
                        ON CONFLICT (FNR) DO UPDATE SET (CV_EKSISTERER, CV_SIST_ENDRET, SISTE_MELDING_MOTTATT)
                        = (excluded.cv_eksisterer, excluded.cv_sist_endret, excluded.siste_melding_mottatt)
                        """,
                fnr.get(), cvEksisterer, cvSistEndret, Timestamp.from(now()));
    }

    public void slettCvRegistrert(Fnr fnr) {
        secureLog.info("Sletter informasjon om at CV finnes for bruker med fnr {}", fnr);
        db.update(String.format("DELETE FROM %s WHERE %s = ?", PostgresTable.BRUKER_REGISTRERT_CV.TABLE_NAME, FNR), fnr.get());
    }
}
