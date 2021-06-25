package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV2 {
    private final JdbcTemplate db;

    public void upsert(Brukertiltak tiltak, AktorId aktorId, String aktivitetId) {
        log.info("Lagrer tiltak: {}", aktivitetId);
        SqlUtils.upsert(db, Table.BRUKERTILTAK_V2.TABLE_NAME)
                .set(Table.BRUKERTILTAK_V2.AKTIVITETID, aktivitetId)
                .set(Table.BRUKERTILTAK_V2.AKTOERID, aktorId.get())
                .set(Table.BRUKERTILTAK_V2.TILTAKSKODE, tiltak.getTiltak())
                .set(Table.BRUKERTILTAK_V2.TILDATO, tiltak.getTildato())
                .where(WhereClause.equals(Table.BRUKERTILTAK_V2.AKTIVITETID, aktivitetId))
                .execute();
    }

    public void delete(String tiltakId) {
        log.info("Sletter tiltak: {}", tiltakId);
        SqlUtils.delete(db, Table.BRUKERTILTAK_V2.TABLE_NAME)
                .where(WhereClause.equals(Table.BRUKERTILTAK_V2.AKTIVITETID, tiltakId))
                .execute();
    }
}
