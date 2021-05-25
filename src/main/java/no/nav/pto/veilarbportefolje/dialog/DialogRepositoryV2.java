package no.nav.pto.veilarbportefolje.dialog;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
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

    public int oppdaterDialogInfoForBruker(Dialogdata dialog) {
        log.info("Oppdaterer dialog i postgres for: {}, med sist endret: {}", dialog.getAktorId(), dialog.getSisteEndring());
        return db.update("INSERT INTO " + TABLE_NAME +
                " (" + SQLINSERT_STRING + ") " +
                "VALUES (" + dialog.toSqlInsertString() + ") " +
                "ON CONFLICT (" + AKTOERID + ") DO UPDATE SET (" + SQLUPDATE_STRING + ") = (" + dialog.toSqlUpdateString() + ")");
    }

    public Optional<Dialogdata> retrieveDialogData(AktorId aktoerId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                db.queryForObject(
                        sql, new Object[]{aktoerId.get()},
                        this::mapToDialogData)
                );
    }

    @SneakyThrows
    private Dialogdata mapToDialogData(ResultSet rs, int i) {
        return new Dialogdata()
                .setAktorId(rs.getString(AKTOERID))
                .setTidspunktEldsteUbehandlede(toZonedDateTime(rs.getTimestamp(VENTER_PA_NAV)))
                .setTidspunktEldsteVentende(toZonedDateTime(rs.getTimestamp(VENTER_PA_BRUKER)));
    }
}
