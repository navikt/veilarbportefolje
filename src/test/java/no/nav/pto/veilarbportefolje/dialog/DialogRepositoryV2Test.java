package no.nav.pto.veilarbportefolje.dialog;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class DialogRepositoryV2Test {
    private JdbcTemplate db;
    private DialogRepositoryV2 dialogRepositoryV2;
    private final AktorId aktoerId = AktorId.of("0");

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        dialogRepositoryV2 = new DialogRepositoryV2(db);

        SqlUtils.delete(db, PostgresTable.DIALOG.TABLE_NAME)
                .where(WhereClause.equals(PostgresTable.OPPFOLGINGSBRUKER_ARENA.AKTOERID, aktoerId.get()))
                .execute();
    }


    @Test
    public void oppdaterDialogInfoForBruker_skal_sette_inn_i_tabell() {
        ZonedDateTime ubehandled = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1000);
        ZonedDateTime ventend = ZonedDateTime.now(ZoneId.of("Europe/Oslo"));

        dialogRepositoryV2.oppdaterDialogInfoForBruker(lagDialogData(ubehandled, ventend));
        Dialogdata dialogFraDatabase = dialogRepositoryV2.retrieveDialogData(aktoerId).get();

        assertThat(dialogFraDatabase.getTidspunktEldsteUbehandlede()).isEqualTo(ubehandled);
        assertThat(dialogFraDatabase.getTidspunktEldsteVentende()).isEqualTo(ventend);
    }

    private Dialogdata lagDialogData(ZonedDateTime ubehandled, ZonedDateTime ventend) {
        return new Dialogdata()
                .setAktorId(aktoerId.get())
                .setTidspunktEldsteUbehandlede(ubehandled)
                .setTidspunktEldsteVentende(ventend);
    }

}
