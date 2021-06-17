package no.nav.pto.veilarbportefolje.cv;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

import static java.time.Instant.now;

import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_CV.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
public class CVRepositoryV2 {

    private final JdbcTemplate db;

    @Autowired
    public CVRepositoryV2(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public int upsert(AktorId aktoerId, boolean harDeltCv) {
        log.info("Oppdater CV for bruker: {}, harDeltCV: {}", aktoerId.get(), harDeltCv);
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

    public Optional<Boolean> harDeltCv(AktorId aktoerId) {
        log.info("Hent delt CV for bruker: {}", aktoerId.get());
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", HAR_DELT_CV, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getBoolean(HAR_DELT_CV), aktoerId.get()))
        );
    }

    public int slettCVData(AktorId aktoerId) {
        log.info("Slett CV for bruker: {}", aktoerId.get());
        return db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktoerId.get());
    }

}
