package no.nav.pto.veilarbportefolje.dialog;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.domene.Endring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.DIALOG.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
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
        Optional<ZonedDateTime> endretDato = getEndretDato(dialog.getAktorId());
        if (dialog.getSisteEndring() == null || (endretDato.isPresent() && endretDato.get().isAfter(dialog.getSisteEndring()))) {
            return;
        }
        db.update("INSERT INTO " + TABLE_NAME +
                " (" + SQLINSERT_STRING + ") " +
                "VALUES (" + dialog.toSqlInsertString() + ") " +
                "ON CONFLICT (" + AKTOERID + ") DO UPDATE SET (" + SQLUPDATE_STRING + ") = (" + dialog.toSqlUpdateString() + ")");
    }

    public Try<Dialogdata> retrieveDialogData(AktorId aktoerId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);
        return Try.of(() -> db.queryForObject(
                sql, new Object[]{aktoerId.get()},
                this::mapToDialogData)
        ).onFailure(e -> {});
    }

    private Optional<ZonedDateTime> getEndretDato(String aktorId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", SIST_OPPDATERT, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilZonedDateTime, aktorId))
        );
    }

    @SneakyThrows
    private ZonedDateTime mapTilZonedDateTime(ResultSet rs, int row){
        return  toZonedDateTime(rs.getTimestamp(SIST_OPPDATERT));
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
