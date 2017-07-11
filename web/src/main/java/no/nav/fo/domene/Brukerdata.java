package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.util.sql.InsertQuery;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.UpdateBatchQuery;
import no.nav.fo.util.sql.UpdateQuery;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class Brukerdata {
    private String aktoerid;
    private String veileder;
    private String personid;
    private LocalDateTime tildeltTidspunkt;
    private YtelseMapping ytelse;
    private LocalDateTime utlopsdato;
    private FasettMapping utlopsFasett;
    private Integer dagputlopUke;
    private DagpengerUkeFasettMapping dagputlopUkeFasett;
    private Integer permutlopUke;
    private DagpengerUkeFasettMapping permutlopUkeFasett;
    private Integer aapmaxtidUke;
    private AAPMaxtidUkeFasettMapping aapmaxtidUkeFasett;

    public UpdateQuery toUpdateQuery(JdbcTemplate db) {
        return SqlUtils.update(db, "bruker_data")
                .set("VEILEDERIDENT", veileder)
                .set("TILDELT_TIDSPUNKT", toTimestamp(tildeltTidspunkt))
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
                .whereEquals("PERSONID", personid);
    }

    public InsertQuery toInsertQuery(JdbcTemplate db) {
        return SqlUtils.insert(db, "bruker_data")
                .value("VEILEDERIDENT", veileder)
                .value("TILDELT_TIDSPUNKT", toTimestamp(tildeltTidspunkt))
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
                .value("PERSONID", personid);
    }


    public static int[] batchUpdate(JdbcTemplate db, List<Brukerdata> data) {
        UpdateBatchQuery<Brukerdata> updateQuery = new UpdateBatchQuery<>(db, "bruker_data");

        return updateQuery
                .add("VEILEDERIDENT", Brukerdata::getVeileder, String.class)
                .add("TILDELT_TIDSPUNKT", (bruker) -> toTimestamp(bruker.tildeltTidspunkt), Timestamp.class)
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
                .addWhereClause("PERSONID", (bruker) -> bruker.personid)
                .execute(data);
    }

    private static Object safeToString(Object o) {
        return o != null ? o.toString() : null;
    }

    private static Timestamp toTimestamp(LocalDateTime localDateTime) {
        return localDateTime != null ? Timestamp.valueOf(localDateTime) : null;
    }
}
