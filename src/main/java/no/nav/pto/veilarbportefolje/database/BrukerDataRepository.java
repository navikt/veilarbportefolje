package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_DATA.*;

@Repository
@RequiredArgsConstructor
public class BrukerDataRepository {
    private final JdbcTemplate db;

    public void upsertAktivitetData(Brukerdata brukerdata) {
        SqlUtils.upsert(db, TABLE_NAME)
                .set(AKTIVITET_START, brukerdata.getAktivitetStart())
                .set(NESTE_AKTIVITET_START, brukerdata.getNesteAktivitetStart())
                .set(FORRIGE_AKTIVITET_START, brukerdata.getForrigeAktivitetStart())
                .set(NYESTEUTLOPTEAKTIVITET, brukerdata.getNyesteUtlopteAktivitet())
                .set(AKTOERID, brukerdata.getAktoerid())
                .set(PERSONID, brukerdata.getPersonid()).where(WhereClause.equals(PERSONID, brukerdata.getPersonid()))
                .execute();
    }
}
