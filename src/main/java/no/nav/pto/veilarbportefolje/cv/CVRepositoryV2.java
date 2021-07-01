package no.nav.pto.veilarbportefolje.cv;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_CV.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CVRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public int upsertHarDeltCv(AktorId aktoerId, boolean harDeltCv) {
        log.info("Oppdater delt CV for bruker: {}, harDeltCV: {}", aktoerId.get(), harDeltCv);
        return db.update("INSERT INTO " + TABLE_NAME +
                        " (" + AKTOERID + ", " + HAR_DELT_CV + ", " + SISTE_MELDING_MOTTATT + ") " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT (" + AKTOERID + ") " +
                        "DO UPDATE SET (" + HAR_DELT_CV + ", " + SISTE_MELDING_MOTTATT + ") = (?, ?)",
                aktoerId.get(),
                harDeltCv,
                Timestamp.from(now()),
                harDeltCv,
                Timestamp.from(now()));
    }

    public int upsertCVEksisterer(AktorId aktoerId, boolean cvEksisterer) {
        log.info("Oppdater CV eksisterer for bruker: {}, eksisterer: {}", aktoerId.get(), cvEksisterer);
        return db.update("INSERT INTO " + TABLE_NAME +
                        " (" + AKTOERID + ", " + CV_EKSISTERER + ", " + SISTE_MELDING_MOTTATT + ") " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT (" + AKTOERID + ") " +
                        "DO UPDATE SET (" + CV_EKSISTERER + ", " + SISTE_MELDING_MOTTATT + ") = (?, ?)",
                aktoerId.get(),
                cvEksisterer,
                Timestamp.from(now()),
                cvEksisterer,
                Timestamp.from(now()));
    }

    public Optional<Boolean> harDeltCv(AktorId aktoerId) {
        log.info("Hent delt CV for bruker: {}", aktoerId.get());
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", HAR_DELT_CV, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getBoolean(HAR_DELT_CV), aktoerId.get()))
        );
    }

    public Optional<Boolean> cvEksisterer(AktorId aktoerId) {
        log.info("Hent CV eksisterer for bruker: {}", aktoerId.get());
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", CV_EKSISTERER, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getBoolean(CV_EKSISTERER), aktoerId.get()))
        );
    }

    public int resetHarDeltCV(AktorId aktoerId) {
        log.info("resetter CV for bruker: {}", aktoerId.get());
        final String updateSql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                TABLE_NAME, HAR_DELT_CV, AKTOERID
        );
        return db.update(updateSql, false, aktoerId.get());
    }

    public int slettCVData(AktorId aktoerId) {
        log.info("Slett CV for bruker: {}", aktoerId.get());
        return db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktoerId.get());
    }

}
