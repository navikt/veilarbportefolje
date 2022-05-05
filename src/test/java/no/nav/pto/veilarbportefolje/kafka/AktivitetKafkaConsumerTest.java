package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.opensearch.action.get.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.Assertions.assertThat;

class AktivitetKafkaConsumerTest extends EndToEndTest {
    private final AktivitetService aktivitetService;

    @Autowired
    public AktivitetKafkaConsumerTest(AktivitetService aktivitetService) {
        this.aktivitetService = aktivitetService;
    }

    @Test
    void skal_oppdatere_aktivitet_i_opensearch() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.setupBruker(aktoerId, ZonedDateTime.now());

        final String tilDato = (LocalDate.now().plusMonths(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toString();
        createAktivitetDocument(aktoerId);

        KafkaAktivitetMelding melding = new KafkaAktivitetMelding()
                .setAktivitetId("1")
                .setAktorId(aktoerId.toString())
                .setFraDato(ZonedDateTime.parse("2020-08-31T10:03:20+02:00"))
                .setTilDato(ZonedDateTime.parse(tilDato))
                .setEndretDato(ZonedDateTime.parse("2020-07-29T15:43:41.049+02:00"))
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setAvtalt(true)
                .setHistorisk(false)
                .setVersion(49179898L);

        aktivitetService.behandleKafkaMeldingLogikk(melding);

        pollOpensearchUntil(() -> aktivitetIJobbUtlopsdatoErOppdatert(aktoerId));

        final String aktivitetIJobbUtlopsdato = getAktivitetIJobbUtlopsdato(opensearchTestClient.fetchDocument(aktoerId));

        assertThat(aktivitetIJobbUtlopsdato).isEqualTo(toIsoUTC(timestampFromISO8601(tilDato)));
    }

    private Boolean aktivitetIJobbUtlopsdatoErOppdatert(AktorId aktoerId) {
        return !Optional.of(opensearchTestClient.fetchDocument(aktoerId))
                .map(AktivitetKafkaConsumerTest::getAktivitetIJobbUtlopsdato)
                .map(utlopsDato -> utlopsDato.equals(getFarInTheFutureDate()))
                .get();
    }

    private static String getAktivitetIJobbUtlopsdato(GetResponse get1) {
        return (String) get1.getSourceAsMap().get("aktivitet_ijobb_utlopsdato");
    }


    private void createAktivitetDocument(AktorId aktoerId) {
        String document = new JSONObject()
                .put("aktivitet_mote_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_stilling_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_egen_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_behandling_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_ijobb_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_sokeavtale_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_tiltak_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_utdanningaktivitet_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_gruppeaktivitet_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktoer_id", aktoerId.toString())
                .toString();

        opensearchTestClient.createDocument(aktoerId, document);
    }
}
