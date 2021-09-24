package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.YtelseMapping;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_DATA.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

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
                .set(PERSONID, brukerdata.getPersonid())
                .where(WhereClause.equals(PERSONID, brukerdata.getPersonid()))
                .execute();
    }

    public void upsertYtelser(Brukerdata brukerdata) {
        YtelseMapping ytelse = brukerdata.getYtelse();
        SqlUtils.upsert(db, TABLE_NAME)
                .set(YTELSE, ytelse != null ? ytelse.toString() : null)
                .set(UTLOPSDATO, toTimestamp(brukerdata.getUtlopsdato()))
                .set(DAGPUTLOPUKE, brukerdata.getDagputlopUke())
                .set(PERMUTLOPUKE, brukerdata.getPermutlopUke())
                .set(AAPMAXTIDUKE, brukerdata.getAapmaxtidUke())
                .set(AAPUNNTAKDAGERIGJEN, brukerdata.getAapUnntakDagerIgjen())
                .set(AKTOERID, brukerdata.getAktoerid())
                .set(PERSONID, brukerdata.getPersonid())
                .where(WhereClause.equals(PERSONID, brukerdata.getPersonid()))
                .execute();
    }

    public List<AktorId> hentBrukereMedUtlopteAktivitetStartDato() {
        String sql = "SELECT " + AKTOERID + " FROM " + TABLE_NAME
                + " WHERE " + AKTIVITET_START + " < CURRENT_TIMESTAMP AND " + AKTOERID + " IS NOT NULL";
        return db.queryForList(sql, String.class).stream()
                .map(AktorId::new)
                .collect(toList());
    }
}
