package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.dialog.Dialogdata;
import no.nav.pto.veilarbportefolje.util.ElasticTestClient;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;

class DialogKafkaConsumerTest extends EndToEndTest {

    private final DialogService dialogService;
    private final ElasticTestClient elasticTestClient;

    @Autowired
    public DialogKafkaConsumerTest(DialogService dialogService, ElasticTestClient elasticTestClient) {
        this.dialogService = dialogService;
        this.elasticTestClient = elasticTestClient;
    }

    @Test
    void skal_oppdatere_dialog_datoer() {
        final AktorId aktoerId = randomAktorId();

        elasticTestClient.createUserInElastic(aktoerId);

        final Dialogdata melding = new Dialogdata().setAktorId(aktoerId.toString())
                .setSisteEndring(ZonedDateTime.parse("2020-10-10T00:00:00+02:00"))
                .setTidspunktEldsteVentende(ZonedDateTime.parse("2020-10-10T00:00:00+02:00"))
                .setTidspunktEldsteUbehandlede(ZonedDateTime.parse("2020-10-10T00:00:00+02:00"));

        dialogService.behandleKafkaMeldingLogikk(melding);
    }
}
