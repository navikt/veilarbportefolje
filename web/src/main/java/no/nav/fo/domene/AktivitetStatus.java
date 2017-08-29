package no.nav.fo.domene;

import lombok.Value;
import lombok.experimental.Wither;
import no.nav.fo.util.DbUtils;
import no.nav.fo.util.sql.UpdateBatchQuery;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;

@Value(staticConstructor = "of")
@Wither
public class AktivitetStatus {
    private PersonId personid;
    private AktoerId aktoerid;
    private String aktivitetType;
    private boolean aktiv;
    private Timestamp nesteUtlop;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AktivitetStatus that = (AktivitetStatus) o;

        return aktivitetType != null ? (aktivitetType).equals(that.aktivitetType) : that.aktivitetType == null;
    }

    @Override
    public int hashCode() {
        return aktivitetType != null ? aktivitetType.hashCode() : 0;
    }

    public static int[] batchUpdate(JdbcTemplate db, List<AktivitetStatus> data) {
        UpdateBatchQuery<AktivitetStatus> updateQuery = new UpdateBatchQuery<>(db, "BRUKERSTATUS_AKTIVITETER");

        return updateQuery
                .add("NESTE_UTLOPSDATO", AktivitetStatus::getNesteUtlop, Timestamp.class)
                .add("STATUS", (a) -> DbUtils.boolTo0OR1(a.isAktiv()), String.class)
                .addWhereClause(
                        (status) -> WhereClause.equals("PERSONID", status.getPersonid().toString())
                                .and(WhereClause.equals("AKTIVITETTYPE", status.getAktivitetType())))
                .execute(data);
    }
}

