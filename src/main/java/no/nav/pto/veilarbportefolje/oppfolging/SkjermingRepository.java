package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.oppfolging.response.SkjermingData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.NOM_SKJERMING.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkjermingRepository {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public Boolean settSkjermingPeriode(Fnr fnr, Timestamp skjermetFra, Timestamp skjermetTil) {
        try {
            int updatedNum = db.update("""
                            INSERT INTO  NOM_SKJERMING(FODSELSNR, SKJERMET_FRA, SKJERMET_TIL) VALUES (?,?,?)
                            ON CONFLICT (FODSELSNR) DO UPDATE SET SKJERMET_FRA = EXCLUDED.SKJERMET_FRA, SKJERMET_TIL = EXCLUDED.SKJERMET_TIL
                            """,
                    fnr.get(), skjermetFra, skjermetTil);
            return updatedNum > 0;
        } catch (Exception e) {
            log.error("Can't set skjerming " + e, e);
            return false;
        }
    }

    public Boolean settSkjerming(Fnr fnr, Boolean erSkjermet) {
        try {
            int updatedNum = db.update("""
                            INSERT INTO NOM_SKJERMING (FODSELSNR, ER_SKJERMET) VALUES (?,?)
                            ON CONFLICT (FODSELSNR) DO UPDATE SET ER_SKJERMET = EXCLUDED.ER_SKJERMET
                            """,
                    fnr.get(), erSkjermet);
            return updatedNum > 0;
        } catch (Exception e) {
            log.error("Can't set skjerming " + e, e);
            return false;
        }
    }

    public Optional<SkjermingData> hentSkjermingData(Fnr fnr) {
        try {
            return Optional.ofNullable(db.queryForObject("""
                            SELECT ER_SKJERMET, SKJERMET_FRA, SKJERMET_TIL FROM NOM_SKJERMING WHERE FODSELSNR = ?
                            """,
                    (rs, rowNum) -> new SkjermingData(fnr, rs.getBoolean(ER_SKJERMET), rs.getTimestamp(SKJERMET_FRA), rs.getTimestamp(SKJERMET_TIL)), fnr.get())
            );
        } catch (Exception e) {
            log.error("Can't get skjerming data " + e, e);
            return Optional.empty();
        }
    }
}
