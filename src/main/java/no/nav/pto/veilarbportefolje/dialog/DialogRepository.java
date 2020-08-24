package no.nav.pto.veilarbportefolje.dialog;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Repository
public class DialogRepository {
    private final JdbcTemplate db;

    @Autowired
    public DialogRepository(JdbcTemplate db) {
        this.db = db;
    }

    private Timestamp toTimestamp(Date date) {
        if(Objects.isNull(date)) {
            return null;
        }
        return new Timestamp(date.toInstant().toEpochMilli());
    }

    public void oppdaterDialogInfoForBruker(Dialogdata dialog) {
        SqlUtils.upsert(db, "DIALOG")
                .set("VENTER_PA_BRUKER", toTimestamp(dialog.getTidspunktEldsteVentende()))
                .set("VENTER_PA_NAV", toTimestamp(dialog.getTidspunktEldsteUbehandlede()))
                .set("OPPDATERT_KILDESYSTEM", toTimestamp(dialog.getSisteEndring()))
                .set("OPPDATERT_PORTEFOLJE", Timestamp.from(Instant.now()))
                .set("AKTOERID", dialog.getAktorId())
                .where(WhereClause.equals("AKTOERID", dialog.getAktorId())).execute();
    }

    public Try<Dialogdata> retrieveDialogData(String aktoerId) {
        return Try.of(() -> db.queryForObject(
                "SELECT * FROM DIALOG WHERE AKTOERID = ?",
                new Object[] {aktoerId},
                this::mapToDialogData)
        ).onFailure(e -> {});
    }

    @SneakyThrows
    private Dialogdata mapToDialogData(ResultSet rs, int i) {
        return new Dialogdata()
                .setAktorId(rs.getString("AKTOERID"))
                .setSisteEndring(rs.getTimestamp("OPPDATERT_KILDESYSTEM"))
                .setTidspunktEldsteUbehandlede(rs.getTimestamp("VENTER_PA_NAV"))
                .setTidspunktEldsteVentende(rs.getTimestamp("VENTER_PA_BRUKER"));
    }
}
