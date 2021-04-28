package no.nav.pto.veilarbportefolje.dialog;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.DIALOG.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
public class DialogRepositoryV2 {
    private final JdbcTemplate db;

    @Autowired
    public DialogRepositoryV2(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public void oppdaterDialogInfoForBruker(Dialogdata dialog) {
        if(getEndretDato().isPresent())
        db.update("INSERT INTO "+ TABLE_NAME
        + " ("+SQLINSERT_STRING+") " +
                "VALUES ("+dialog.toSqlInsertString()+ ") " +
                "ON CONFLICT (" + AKTOERID + ") DO UPDATE SET (" + SQLUPDATE_STRING + ") = (" + dialog.toSqlUpdateString() + ")");
    }

    public Try<Dialogdata> retrieveDialogData(String aktoerId) {
        return Try.of(() -> db.queryForObject(
                "SELECT * FROM DIALOG WHERE AKTOERID = ?",
                new Object[] {aktoerId},
                this::mapToDialogData)
        ).onFailure(e -> {});
    }

    private Optional<Timestamp> getEndretDato(String aktorId) {
        return Optional.ofNullable(
                db.queryForObject("SELECT * FROM DIALOG WHERE AKTOERID = "+aktorId, Timestamp.class)
        );
    }

    @SneakyThrows
    private Dialogdata mapToDialogData(ResultSet rs, int i) {
        return new Dialogdata()
                .setAktorId(rs.getString(AKTOERID))
                .setSisteEndring(toZonedDateTime(rs.getTimestamp(SIST_OPPDATERT)))
                .setTidspunktEldsteUbehandlede(toZonedDateTime(rs.getTimestamp(VENTER_PA_NAV)))
                .setTidspunktEldsteVentende(toZonedDateTime(rs.getTimestamp(VENTER_PA_BRUKER)));
    }
}
