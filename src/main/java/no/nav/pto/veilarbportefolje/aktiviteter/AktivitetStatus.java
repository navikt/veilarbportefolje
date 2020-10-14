package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.util.DbUtils;
import no.nav.sbl.sql.InsertBatchQuery;
import no.nav.sbl.sql.UpdateBatchQuery;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Data
@Accessors(chain = true)
public class AktivitetStatus {
    PersonId personid;
    AktoerId aktoerid;
    String aktivitetType;
    boolean aktiv;
    Timestamp nesteUtlop;
    Timestamp nesteStart;

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
                .add("NESTE_STARTDATO", AktivitetStatus::getNesteStart, Timestamp.class)
                .add("STATUS", (a) -> DbUtils.boolTo0OR1(a.isAktiv()), String.class)
                .addWhereClause(
                        (status) -> WhereClause.equals("PERSONID", status.getPersonid().toString())
                                .and(WhereClause.equals("AKTIVITETTYPE", status.getAktivitetType())))
                .execute(data);
    }

    public static int[] batchInsert(JdbcTemplate db, List<AktivitetStatus> data) throws SQLIntegrityConstraintViolationException {
        InsertBatchQuery<AktivitetStatus> insertQuery = new InsertBatchQuery<>(db, "BRUKERSTATUS_AKTIVITETER");

        return insertQuery
                .add("PERSONID", (a) -> a.getPersonid().toString(), String.class)
                .add("AKTOERID", AktivitetStatus::aktoeridOrElseNull, String.class)
                .add("AKTIVITETTYPE", AktivitetStatus::getAktivitetType, String.class)
                .add("STATUS", (a) -> DbUtils.boolTo0OR1(a.isAktiv()), String.class)
                .add("NESTE_UTLOPSDATO", AktivitetStatus::getNesteUtlop, Timestamp.class)
                .add("NESTE_STARTDATO", AktivitetStatus::getNesteStart, Timestamp.class)
                .execute(data);
    }

    private String aktoeridOrElseNull() {
        return Optional.ofNullable(aktoerid)
                .map(AktoerId::toString)
                .orElse(null);
    }
}

