package no.nav.pto.veilarbportefolje.arenaaktiviteter;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.Brukertiltak;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV2 {
    private final JdbcTemplate db;

    public void insert(Brukertiltak tiltak, AktorId aktorId) {
        SqlUtils.upsert(db, Table.BRUKERTILTAK_V2.TABLE_NAME)
                .set(Table.BRUKERTILTAK_V2.AKTOERID, aktorId.get())
                .set(Table.BRUKERTILTAK_V2.TILTAKSKODE, tiltak.getTiltak())
                .set(Table.BRUKERTILTAK_V2.TILDATO, tiltak.getTildato())
                .where(WhereClause.equals(Table.BRUKERTILTAK_V2.AKTOERID, aktorId.get()))
                .execute();
    }
}
