package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.oppfolging.response.SkjermingData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.NOM_SKJERMING.ER_SKJERMET;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.NOM_SKJERMING.FNR;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.NOM_SKJERMING.SKJERMET_FRA;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.NOM_SKJERMING.SKJERMET_TIL;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkjermingRepository {
    private final JdbcTemplate db;

    public boolean settSkjermingPeriode(Fnr fnr, Timestamp skjermetFra, Timestamp skjermetTil) {
        int updatedNum = db.update("""
                        INSERT INTO  NOM_SKJERMING(FODSELSNR, SKJERMET_FRA, SKJERMET_TIL) VALUES (?,?,?)
                        ON CONFLICT (FODSELSNR) DO UPDATE SET SKJERMET_FRA = EXCLUDED.SKJERMET_FRA, SKJERMET_TIL = EXCLUDED.SKJERMET_TIL
                        """,
                fnr.get(), skjermetFra, skjermetTil);
        return updatedNum > 0;
    }

    public boolean settSkjerming(Fnr fnr, Boolean erSkjermet) {
        int updatedNum = db.update("""
                        INSERT INTO NOM_SKJERMING (FODSELSNR, ER_SKJERMET) VALUES (?,?)
                        ON CONFLICT (FODSELSNR) DO UPDATE SET ER_SKJERMET = EXCLUDED.ER_SKJERMET
                        """,
                fnr.get(), erSkjermet);
        return updatedNum > 0;
    }

    public Optional<SkjermingData> hentSkjermingData(Fnr fnr) {
        return Optional.ofNullable(db.queryForObject("""
                        SELECT ER_SKJERMET, SKJERMET_FRA, SKJERMET_TIL FROM NOM_SKJERMING WHERE FODSELSNR = ?
                        """,
                (rs, rowNum) -> new SkjermingData(fnr, rs.getBoolean(ER_SKJERMET), rs.getTimestamp(SKJERMET_FRA), rs.getTimestamp(SKJERMET_TIL)), fnr.get())
        );
    }


    public Set<Fnr> hentSkjermetPersoner(List<Fnr> fnrs) {
        String fnrsCondition = fnrs.stream().map(Fnr::toString).collect(Collectors.joining(",", "{", "}"));
        Set<Fnr> skjermetPersoner = new HashSet<>();
        db.query("""
                        SELECT FODSELSNR FROM NOM_SKJERMING WHERE ER_SKJERMET AND FODSELSNR = ANY (?::varchar[])
                        """,
                ps -> ps.setString(1, fnrsCondition),
                (ResultSet rs) -> {
                    skjermetPersoner.add(Fnr.of(rs.getString(FNR)));
                }
        );
        return skjermetPersoner;
    }

    public void deleteSkjermingData(Fnr fnr) {
        db.update("""
                DELETE FROM NOM_SKJERMING WHERE FODSELSNR = ?
                """, fnr.get());
    }
}
