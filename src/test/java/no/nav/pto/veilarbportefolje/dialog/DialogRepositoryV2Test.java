package no.nav.pto.veilarbportefolje.dialog;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolginsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
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
        ZonedDateTime endringsDato = ZonedDateTime.now(ZoneId.of("Europe/Oslo"));
        ZonedDateTime venteDato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1000);

        dialogRepositoryV2.oppdaterDialogInfoForBruker(lagDialogData(venteDato, endringsDato));
        Dialogdata dialogFraDatabase = dialogRepositoryV2.retrieveDialogData(aktoerId).get();

        assertThat(dialogFraDatabase.getSisteEndring()).isEqualTo(endringsDato);
        assertThat(dialogFraDatabase.getTidspunktEldsteVentende()).isEqualTo(venteDato);
        assertThat(dialogFraDatabase.getTidspunktEldsteUbehandlede()).isEqualTo(venteDato);
    }


    @Test
    public void oppdaterDialogInfoForBruker_skal_ikke_sette_inn_gamel_dialog() {
        ZonedDateTime endringsDato = ZonedDateTime.now(ZoneId.of("Europe/Oslo"));
        ZonedDateTime endringsDatoGamel = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(1);
        ZonedDateTime venteDato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1000);

        dialogRepositoryV2.oppdaterDialogInfoForBruker(lagDialogData(venteDato, endringsDato));
        dialogRepositoryV2.oppdaterDialogInfoForBruker(lagDialogData(venteDato, endringsDatoGamel));
        Dialogdata dialogFraDatabase2 = dialogRepositoryV2.retrieveDialogData(aktoerId).get();

        assertThat(dialogFraDatabase2.getSisteEndring()).isEqualTo(endringsDato);
    }

    private Dialogdata lagDialogData(ZonedDateTime venteDato, ZonedDateTime endringsDato) {
        return new Dialogdata()
                .setAktorId(aktoerId.get())
                .setTidspunktEldsteUbehandlede(venteDato)
                .setSisteEndring(endringsDato)
                .setTidspunktEldsteVentende(venteDato);
    }

}
