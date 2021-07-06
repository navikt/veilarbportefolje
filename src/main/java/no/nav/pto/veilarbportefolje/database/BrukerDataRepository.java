package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
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

    public List<AktorId> hentBrukereMedUtlopteAktivitetStartDato() {
        String sql = "SELECT " + AKTOERID + " FROM " + TABLE_NAME
                + " WHERE " + AKTIVITET_START + " < CURRENT_TIMESTAMP";
        return db.queryForList(sql, String.class).stream()
                .filter(Objects::nonNull)
                .map(AktorId::new)
                .collect(toList());
    }
}
