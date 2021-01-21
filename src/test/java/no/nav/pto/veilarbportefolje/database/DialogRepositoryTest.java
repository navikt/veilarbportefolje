package no.nav.pto.veilarbportefolje.database;


import no.nav.pto.veilarbportefolje.dialog.Dialogdata;
import no.nav.pto.veilarbportefolje.dialog.DialogRepository;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static org.assertj.core.api.Assertions.assertThat;


public class DialogRepositoryTest {


    private DialogRepository dialogRepository;

    @Before
    public void setup() {
        dialogRepository = new DialogRepository(new JdbcTemplate(setupInMemoryDatabase()));
    }

    private static final AktoerId AKTOER_ID = AktoerId.of("1111");

    @Test
    public void oppdaterDialogInfoForBruker_skal_sette_inn_i_tabell_og_vare_tilgjengelig_i_dialogview() {
        long now = Instant.now().toEpochMilli();
        ZonedDateTime endringsDato = ZonedDateTime.now(ZoneId.of("Europe/Oslo"));
        ZonedDateTime venteDato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMinutes(1000);

        dialogRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, endringsDato));
        Dialogdata dialogFraDatabase = dialogRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, endringsDato);
    }

    private Dialogdata lagDialogData(ZonedDateTime venteDato, ZonedDateTime endringsDato) {
        return new Dialogdata()
                .setAktorId(AKTOER_ID.toString())
                .setTidspunktEldsteUbehandlede(venteDato)
                .setSisteEndring(endringsDato)
                .setTidspunktEldsteVentende(venteDato);
    }

    private void verifiserData(ZonedDateTime date, Dialogdata dialogFraDatabase, ZonedDateTime endringsDato) {
        assertThat(dialogFraDatabase.getTidspunktEldsteVentende()).isEqualTo(date);
        assertThat(dialogFraDatabase.getTidspunktEldsteUbehandlede()).isEqualTo(date);
        assertThat(dialogFraDatabase.getSisteEndring()).isEqualTo(endringsDato);
        assertThat(dialogFraDatabase.getAktorId()).isEqualTo(AKTOER_ID.toString());
    }

    @Test
    public void oppdaterDialogInfoForBruker_skal_oppdatere_tabell_og_vare_tilgjengelig_i_dialogview() {
        ZonedDateTime venteDato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusSeconds(1);

        dialogRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, venteDato));
        Dialogdata dialogFraDatabase = dialogRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, venteDato);

        ZonedDateTime nyEndringsDato = ZonedDateTime.now(ZoneId.of("Europe/Oslo"));
        dialogRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, nyEndringsDato));
        dialogFraDatabase = dialogRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, nyEndringsDato);
    }
}
