package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.UpdateBatchQuery;
import no.nav.fo.util.sql.UpsertQuery;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
    private Integer aapUnntakDagerIgjen;
    private AAPUnntakUkerIgjenFasettMapping aapunntakUkerIgjenFasett;
    private LocalDateTime venterPaSvarFraBruker;
    private LocalDateTime venterPaSvarFraNav;
    private Boolean oppfolging;
    private Boolean nyForVeileder;
    private Boolean nyForEnhet;
    private Timestamp nyesteUtlopteAktivitet;
    private Timestamp aktivitetStart;
    private Timestamp nesteAktivitetStart;
    private Timestamp forrigeAktivitetStart;
    private Set<AktivitetStatus> aktiviteter;

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
                .set("AAPUNNTAKDAGERIGJEN", safeToString(aapUnntakDagerIgjen))
                .set("AAPUNNTAKUKERIGJENFASETT", safeToString(aapunntakUkerIgjenFasett))
                .set("VENTERPASVARFRABRUKER", toTimestamp(venterPaSvarFraBruker))
                .set("VENTERPASVARFRANAV", toTimestamp(venterPaSvarFraNav))
                .set("PERSONID", personid)
                .set("OPPFOLGING", safeToJaNei(oppfolging))
                .set("NY_FOR_VEILEDER", safeToJaNei(nyForVeileder))
                .set("NY_FOR_ENHET", safeToJaNei(nyForEnhet))
                .set("NYESTEUTLOPTEAKTIVITET", nyesteUtlopteAktivitet)
                .set("AKTIVITET_START", aktivitetStart)
                .set("NESTE_AKTIVITET_START", nesteAktivitetStart)
                .set("FORRIGE_AKTIVITET_START", forrigeAktivitetStart);
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
                .add("AAPUNNTAKDAGERIGJEN", (bruker) -> bruker.aapUnntakDagerIgjen, Integer.class)
                .add("AAPUNNTAKUKERIGJENFASETT", (bruker) -> safeToString(bruker.aapunntakUkerIgjenFasett), String.class)
                .add("OPPFOLGING", (bruker) -> safeToJaNei(bruker.oppfolging), String.class)
                .add("NY_FOR_VEILEDER", (bruker) -> safeToJaNei(bruker.nyForVeileder), String.class)
                .add("NY_FOR_ENHET", (bruker) -> safeToJaNei(bruker.nyForEnhet), String.class)
                .add("VENTERPASVARFRABRUKER", (bruker) -> toTimestamp(bruker.venterPaSvarFraBruker), Timestamp.class)
                .add("VENTERPASVARFRANAV", (bruker) -> toTimestamp(bruker.venterPaSvarFraNav), Timestamp.class)
                .add("NYESTEUTLOPTEAKTIVITET", (bruker) ->  bruker.nyesteUtlopteAktivitet, Timestamp.class)
                .add("AKTIVITET_START", (bruker) ->  bruker.aktivitetStart, Timestamp.class)
                .add("NESTE_AKTIVITET_START", (bruker) ->  bruker.nesteAktivitetStart, Timestamp.class)
                .add("FORRIGE_AKTIVITET_START", (bruker) ->  bruker.forrigeAktivitetStart, Timestamp.class)
                .addWhereClause((bruker) -> WhereClause.equals("PERSONID",bruker.personid))
                .execute(data);
    }

    public static String safeToJaNei(Boolean aBoolean) {
        if (aBoolean == null) {
            return "N";
        }
        return aBoolean ? "J" : "N";
    }

    private static Object safeToString(Object o) {
        return o != null ? o.toString() : null;
    }

    private static Timestamp toTimestamp(LocalDateTime localDateTime) {
        return localDateTime != null ? Timestamp.valueOf(localDateTime) : null;
    }
}
