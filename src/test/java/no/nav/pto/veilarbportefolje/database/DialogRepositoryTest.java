package no.nav.pto.veilarbportefolje.database;


import no.nav.pto.veilarbportefolje.dialog.DialogData;
import no.nav.pto.veilarbportefolje.dialog.DialogFeedRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;


public class DialogRepositoryTest {


    private DialogFeedRepository dialogFeedRepository;

    @BeforeEach
    public void setup() {
        dialogFeedRepository = new DialogFeedRepository(new JdbcTemplate(setupInMemoryDatabase()));
    }

    private static final AktoerId AKTOER_ID = AktoerId.of("1111");

    @Test
    public void oppdaterDialogInfoForBruker_skal_sette_inn_i_tabell_og_vare_tilgjengelig_i_dialogview() {
        long now = Instant.now().toEpochMilli();
        Timestamp endringsDato = new Timestamp(now);
        Timestamp venteDato = new Timestamp(now - 1000);

        dialogFeedRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, endringsDato));
        DialogData dialogFraDatabase = dialogFeedRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, endringsDato);
    }

    private DialogData lagDialogData(Timestamp venteDato, Timestamp endringsDato) {
        return new DialogData()
                .setAktorId(AKTOER_ID.toString())
                .setTidspunktEldsteUbehandlede(venteDato)
                .setSisteEndring(endringsDato)
                .setTidspunktEldsteVentende(venteDato);
    }

    private void verifiserData(Timestamp date, DialogData dialogFraDatabase, Timestamp endringsDato) {
        assertThat(dialogFraDatabase.getTidspunktEldsteVentende(), is(date));
        assertThat(dialogFraDatabase.getTidspunktEldsteUbehandlede(), is(date));
        assertThat(dialogFraDatabase.getSisteEndring(), is(endringsDato));
        assertThat(dialogFraDatabase.getAktorId(), is(AKTOER_ID.toString()));
    }

    @Test
    public void oppdaterDialogInfoForBruker_skal_oppdatere_tabell_og_vare_tilgjengelig_i_dialogview() {
        Timestamp venteDato = new Timestamp(Instant.now().toEpochMilli()-1000);

        dialogFeedRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, venteDato));
        DialogData dialogFraDatabase = dialogFeedRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, venteDato);

        Timestamp nyEndringsDato = new Timestamp(Instant.now().toEpochMilli());
        dialogFeedRepository.oppdaterDialogInfoForBruker(lagDialogData(venteDato, nyEndringsDato));
        dialogFraDatabase = dialogFeedRepository.retrieveDialogData(AKTOER_ID.toString()).get();
        verifiserData(venteDato, dialogFraDatabase, nyEndringsDato);
    }
}
