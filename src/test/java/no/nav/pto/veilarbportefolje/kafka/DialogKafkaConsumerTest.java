package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.dialog.DialogdataDto;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;

public class DialogKafkaConsumerTest extends EndToEndTest {

    private final DialogService dialogService;

    @Autowired
    public DialogKafkaConsumerTest(DialogService dialogService, OpensearchTestClient opensearchTestClient) {
        this.dialogService = dialogService;
        this.opensearchTestClient = opensearchTestClient;
    }

    @Test
    public void skal_oppdatere_dialog_datoer() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.lagreBrukerUnderOppfolging(aktoerId, ZonedDateTime.now());

        final DialogdataDto melding = new DialogdataDto().setAktorId(aktoerId.toString())
                .setSisteEndring(ZonedDateTime.parse("2020-10-10T00:00:00+02:00"))
                .setTidspunktEldsteVentende(ZonedDateTime.parse("2020-10-10T00:00:00+02:00"))
                .setTidspunktEldsteUbehandlede(ZonedDateTime.parse("2020-10-10T00:00:00+02:00"));

        dialogService.behandleKafkaMeldingLogikk(melding);
    }
}
