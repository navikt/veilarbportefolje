package no.nav.pto.veilarbportefolje.dialog;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.DIALOG.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.DIALOG.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.DIALOG.VENTER_PA_BRUKER;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.DIALOG.VENTER_PA_NAV;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DialogRepositoryV2 {
    private final JdbcTemplate db;

    public void oppdaterDialogInfoForBruker(Dialogdata dialog) {
        db.update("""
                        INSERT INTO dialog (AKTOERID, VENTER_PA_BRUKER, VENTER_PA_NAV)
                        VALUES (?, ?, ?) ON CONFLICT (AKTOERID)
                        DO UPDATE SET (VENTER_PA_BRUKER, VENTER_PA_NAV)
                        = (excluded.venter_pa_bruker, excluded.venter_pa_nav)
                        """,
                dialog.getAktorId(), toTimestamp(dialog.getTidspunktEldsteVentende()), toTimestamp(dialog.getTidspunktEldsteUbehandlede())
        );

    }

    public Optional<Dialogdata> retrieveDialogData(AktorId aktoerId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapToDialogData, aktoerId.get()))
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
