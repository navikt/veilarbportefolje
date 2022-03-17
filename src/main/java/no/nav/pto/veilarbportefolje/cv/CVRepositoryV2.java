package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_CV.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_CV.CV_EKSISTERER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_CV.HAR_DELT_CV;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_CV.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CVRepositoryV2 {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public int upsertHarDeltCv(AktorId aktoerId, boolean harDeltCv) {
        return db.update("""
                        INSERT INTO bruker_cv
                        (AKTOERID, HAR_DELT_CV, SISTE_MELDING_MOTTATT) VALUES (?, ?, ?)
                        ON CONFLICT (AKTOERID) DO UPDATE SET (HAR_DELT_CV, SISTE_MELDING_MOTTATT)
                        = (excluded.har_delt_cv, excluded.siste_melding_mottatt)
                        """,
                aktoerId.get(), harDeltCv, Timestamp.from(now()));
    }

    public int upsertCVEksisterer(AktorId aktoerId, boolean cvEksisterer) {
        return db.update("""
                        INSERT INTO bruker_cv
                        (AKTOERID, CV_EKSISTERER, SISTE_MELDING_MOTTATT) VALUES (?, ?, ?)
                        ON CONFLICT (AKTOERID) DO UPDATE SET (CV_EKSISTERER, SISTE_MELDING_MOTTATT)
                        = (excluded.cv_eksisterer, excluded.siste_melding_mottatt)
                        """,
                aktoerId.get(), cvEksisterer, Timestamp.from(now()));
    }

    public boolean harDeltCv(AktorId aktoerId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", HAR_DELT_CV, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getBoolean(HAR_DELT_CV), aktoerId.get()))
        ).orElse(false);
    }

    public boolean cvEksisterer(AktorId aktoerId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", CV_EKSISTERER, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getBoolean(CV_EKSISTERER), aktoerId.get()))
        ).orElse(false);
    }

    public int resetHarDeltCV(AktorId aktoerId) {
        log.info("resetter CV for bruker: {}", aktoerId.get());
        final String updateSql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                TABLE_NAME, HAR_DELT_CV, AKTOERID
        );
        return db.update(updateSql, false, aktoerId.get());
    }

    public Set<AktorId> hentAlleBrukereSomHarCV() {
        Set<AktorId> alleBrukereSomHarCv = new HashSet<>();
        db.query("select aktoerid from bruker_cv where cv_eksisterer",
                (rs, i) -> alleBrukereSomHarCv.add(AktorId.of(rs.getString("aktoerid")))
        );
        return alleBrukereSomHarCv;
    }

    public Set<AktorId> hentAlleBrukereSomHarSettHjemmel() {
        Set<AktorId> alleBrukereSomHarSettHjemmel = new HashSet<>();
        db.query("select aktoerid from bruker_cv where har_delt_cv",
                (rs, i) -> alleBrukereSomHarSettHjemmel.add(AktorId.of(rs.getString("aktoerid")))
        );
        return alleBrukereSomHarSettHjemmel;
    }
}
