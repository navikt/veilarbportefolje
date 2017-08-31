package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.util.sql.*;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class Brukerdata {
    private String aktoerid;
    private String veileder;
    private String personid;
    private Timestamp tildeltTidspunkt;
    private YtelseMapping ytelse;
    private LocalDateTime utlopsdato;
    private FasettMapping utlopsFasett;
    private Integer dagputlopUke;
    private DagpengerUkeFasettMapping dagputlopUkeFasett;
    private Integer permutlopUke;
    private DagpengerUkeFasettMapping permutlopUkeFasett;
    private Integer aapmaxtidUke;
    private AAPMaxtidUkeFasettMapping aapmaxtidUkeFasett;
    private LocalDateTime venterPaSvarFraBruker;
    private LocalDateTime venterPaSvarFraNav;
    private Boolean oppfolging;
    private Boolean iAvtaltAktivitet;
    private Timestamp nyesteUtlopteAktivitet;
    private Map<String, Boolean> aktivitetStatus;

    public UpdateQuery toUpdateQuery(JdbcTemplate db) {
        return SqlUtils.update(db, "bruker_data")
                .set("VEILEDERIDENT", veileder)
                .set("TILDELT_TIDSPUNKT", tildeltTidspunkt)
                .set("AKTOERID", aktoerid)
                .set("YTELSE", safeToString(ytelse))
                .set("UTLOPSDATO", toTimestamp(utlopsdato))
                .set("UTLOPSDATOFASETT", safeToString(utlopsFasett))
                .set("DAGPUTLOPUKE", safeToString(dagputlopUke))
                .set("DAGPUTLOPUKEFASETT", safeToString(dagputlopUkeFasett))
                .set("PERMUTLOPUKE", safeToString(permutlopUke))
                .set("PERMUTLOPUKEFASETT", safeToString(permutlopUkeFasett))
                .set("AAPMAXTIDUKE", safeToString(aapmaxtidUke))
                .set("AAPMAXTIDUKEFASETT", safeToString(aapmaxtidUkeFasett))
                .set("OPPFOLGING", safeToJaNei(oppfolging))
                .set("VENTERPASVARFRABRUKER", toTimestamp(venterPaSvarFraBruker))
                .set("VENTERPASVARFRANAV", toTimestamp(venterPaSvarFraNav))
                .whereEquals("PERSONID", personid);
    }

    public UpsertQuery toUpsertQuery(JdbcTemplate db) {
        return SqlUtils.upsert(db, "bruker_data")
                .where(WhereClause.equals("PERSONID", personid))
                .set("VEILEDERIDENT", veileder)
                .set("TILDELT_TIDSPUNKT", tildeltTidspunkt)
                .set("AKTOERID", aktoerid)
                .set("YTELSE", ytelse != null ? ytelse.toString() : null)
                .set("UTLOPSDATO", toTimestamp(utlopsdato))
                .set("UTLOPSDATOFASETT", safeToString(utlopsFasett))
                .set("DAGPUTLOPUKE", safeToString(dagputlopUke))
                .set("DAGPUTLOPUKEFASETT", safeToString(dagputlopUkeFasett))
                .set("PERMUTLOPUKE", safeToString(permutlopUke))
                .set("PERMUTLOPUKEFASETT", safeToString(permutlopUkeFasett))
                .set("AAPMAXTIDUKE", safeToString(aapmaxtidUke))
                .set("AAPMAXTIDUKEFASETT", safeToString(aapmaxtidUkeFasett))
                .set("VENTERPASVARFRABRUKER", toTimestamp(venterPaSvarFraBruker))
                .set("VENTERPASVARFRANAV", toTimestamp(venterPaSvarFraNav))
                .set("PERSONID", personid)
                .set("OPPFOLGING", safeToJaNei(oppfolging))
                .set("IAVTALTAKTIVITET", booleanTo0OR1(iAvtaltAktivitet))
                .set("NYESTEUTLOPTEAKTIVITET", nyesteUtlopteAktivitet);
    }

    public InsertQuery toInsertQuery(JdbcTemplate db) {
        return SqlUtils.insert(db, "bruker_data")
                .value("VEILEDERIDENT", veileder)
                .value("TILDELT_TIDSPUNKT", tildeltTidspunkt)
                .value("AKTOERID", aktoerid)
                .value("YTELSE", safeToString(ytelse))
                .value("UTLOPSDATO", toTimestamp(utlopsdato))
                .value("UTLOPSDATOFASETT", safeToString(utlopsFasett))
                .value("DAGPUTLOPUKE", safeToString(dagputlopUke))
                .value("DAGPUTLOPUKEFASETT", safeToString(dagputlopUkeFasett))
                .value("PERMUTLOPUKE", safeToString(permutlopUke))
                .value("PERMUTLOPUKEFASETT", safeToString(permutlopUkeFasett))
                .value("AAPMAXTIDUKE", safeToString(aapmaxtidUke))
                .value("AAPMAXTIDUKEFASETT", safeToString(aapmaxtidUkeFasett))
                .value("PERSONID", personid)
                .value("VENTERPASVARFRABRUKER", toTimestamp(venterPaSvarFraBruker))
                .value("VENTERPASVARFRANAV", toTimestamp(venterPaSvarFraNav))
                .value("OPPFOLGING", safeToJaNei(oppfolging))
                .value("IAVTALTAKTIVITET", booleanTo0OR1(iAvtaltAktivitet))
                .value("NYESTEUTLOPTEAKTIVITET", nyesteUtlopteAktivitet);
    }

    public static int[] batchUpdate(JdbcTemplate db, List<Brukerdata> data) {
        UpdateBatchQuery<Brukerdata> updateQuery = new UpdateBatchQuery<>(db, "bruker_data");

        return updateQuery
                .add("VEILEDERIDENT", Brukerdata::getVeileder, String.class)
                .add("TILDELT_TIDSPUNKT", (bruker) -> bruker.tildeltTidspunkt, Timestamp.class)
                .add("AKTOERID", Brukerdata::getAktoerid, String.class)
                .add("YTELSE", (bruker) -> safeToString(bruker.ytelse), String.class)
                .add("UTLOPSDATO", (bruker) -> toTimestamp(bruker.utlopsdato), Timestamp.class)
                .add("UTLOPSDATOFASETT", (bruker) -> safeToString(bruker.utlopsFasett), String.class)
                .add("DAGPUTLOPUKE", (bruker) -> bruker.dagputlopUke, Integer.class)
                .add("DAGPUTLOPUKEFASETT", (bruker) -> safeToString(bruker.dagputlopUkeFasett), String.class)
                .add("PERMUTLOPUKE", (bruker) -> bruker.permutlopUke, Integer.class)
                .add("PERMUTLOPUKEFASETT", (bruker) -> safeToString(bruker.permutlopUkeFasett), String.class)
                .add("AAPMAXTIDUKE", (bruker) -> bruker.aapmaxtidUke, Integer.class)
                .add("AAPMAXTIDUKEFASETT", (bruker) -> safeToString(bruker.aapmaxtidUkeFasett), String.class)
                .add("OPPFOLGING", (bruker) -> safeToJaNei(bruker.oppfolging), String.class)
                .add("VENTERPASVARFRABRUKER", (bruker) -> toTimestamp(bruker.venterPaSvarFraBruker), Timestamp.class)
                .add("VENTERPASVARFRANAV", (bruker) -> toTimestamp(bruker.venterPaSvarFraNav), Timestamp.class)
                .add("IAVTALTAKTIVITET", (bruker) -> booleanTo0OR1(bruker.iAvtaltAktivitet), String.class)
                .add("NYESTEUTLOPTEAKTIVITET", (bruker) ->  bruker.nyesteUtlopteAktivitet, Timestamp.class)
                .addWhereClause("PERSONID", (bruker) -> bruker.personid)
                .execute(data);
    }

    public static String safeToJaNei(Boolean oppfolging) {
        if (oppfolging == null) {
            return "N";
        }
        return oppfolging ? "J" : "N";
    }

    private static String booleanTo0OR1(Boolean bool) {
        if(bool == null) {
            return null;
        }
        return bool ? "1" : "0";
    }

    private static Object safeToString(Object o) {
        return o != null ? o.toString() : null;
    }

    private static Timestamp toTimestamp(LocalDateTime localDateTime) {
        return localDateTime != null ? Timestamp.valueOf(localDateTime) : null;
    }
}
