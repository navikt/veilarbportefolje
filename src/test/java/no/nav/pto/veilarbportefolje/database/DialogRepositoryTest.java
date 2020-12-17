package no.nav.pto.veilarbportefolje.database;


import no.nav.pto.veilarbportefolje.dialog.Dialogdata;
import no.nav.pto.veilarbportefolje.dialog.DialogRepository;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
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
        Timestamp endringsDato = new Timestamp(now);
        Timestamp venteDato = new Timestamp(now - 1000);

        dialogRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, endringsDato));
        Dialogdata dialogFraDatabase = dialogRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, endringsDato);
    }

    private Dialogdata lagDialogData(Timestamp venteDato, Timestamp endringsDato) {
        return new Dialogdata()
                .setAktorId(AKTOER_ID.toString())
                .setTidspunktEldsteUbehandlede(venteDato)
                .setSisteEndring(endringsDato)
                .setTidspunktEldsteVentende(venteDato);
    }

    private void verifiserData(Timestamp date, Dialogdata dialogFraDatabase, Timestamp endringsDato) {
        assertThat(dialogFraDatabase.getTidspunktEldsteVentende()).isEqualTo(date);
        assertThat(dialogFraDatabase.getTidspunktEldsteUbehandlede()).isEqualTo(date);
        assertThat(dialogFraDatabase.getSisteEndring()).isEqualTo(endringsDato);
        assertThat(dialogFraDatabase.getAktorId()).isEqualTo(AKTOER_ID.toString());
    }

    @Test
    public void oppdaterDialogInfoForBruker_skal_oppdatere_tabell_og_vare_tilgjengelig_i_dialogview() {
        Timestamp venteDato = new Timestamp(Instant.now().toEpochMilli()-1000);

        dialogRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, venteDato));
        Dialogdata dialogFraDatabase = dialogRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, venteDato);

        Timestamp nyEndringsDato = new Timestamp(Instant.now().toEpochMilli());
        dialogRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, nyEndringsDato));
        dialogFraDatabase = dialogRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, nyEndringsDato);
    }
}
