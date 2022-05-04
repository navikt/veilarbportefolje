package no.nav.pto.veilarbportefolje.dialog;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.util.DateUtils.now;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.Assertions.assertThat;

public class DialogRepositoryV2Test {
    private DialogRepositoryV2 dialogRepositoryV2;
    private final AktorId aktoerId = randomAktorId();

    @Before
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        dialogRepositoryV2 = new DialogRepositoryV2(db);
    }


    @Test
    public void oppdaterDialogInfoForBruker_skal_sette_inn_i_tabell() {
        ZonedDateTime ubehandled = now().minusMinutes(1000);
        ZonedDateTime ventend = now();

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
