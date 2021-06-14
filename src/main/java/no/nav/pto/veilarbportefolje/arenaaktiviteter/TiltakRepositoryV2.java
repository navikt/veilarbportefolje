package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.sbl.sql.InsertBatchQuery;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV2 {
    private final JdbcTemplate db;

    public void batchInsert(JdbcTemplate db, Brukertiltak data) {
        InsertBatchQuery<Brukertiltak> insertQuery = new InsertBatchQuery<>(db, "BRUKERTILTAK");
        SqlUtils.upsert(db, Table.BRUKERTILTAK.TABLE_NAME)
                .set(Table.BRUKERTILTAK.FODSELSNR, data.getFnr())
                .set(Table.BRUKERTILTAK.TILTAKSKODE, data.getTiltak())
                .set(Table.BRUKERTILTAK.TILDATO, data.getTildato())
                .where(WhereClause.equals(Table.BRUKERTILTAK.FODSELSNR, data.getFnr()))
                .execute();
    }
}
