package no.nav.pto.veilarbportefolje.aktiviteter;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;
import org.junit.Test;

import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.common.json.JsonUtils.fromJson;
import static org.assertj.core.api.Assertions.assertThat;

public class KafkaAktivtetServiceTest {
    @Test
    public void skal_deserialisere_kafka_payload() {
        String aktivitetKafkaMeldingMedVersion = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"aktorId\":\"123456789\"," +
                "\"version\":\"1\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                "\"endretDato\":\"2020-05-28T09:47:42.48+02:00\"," +
                "\"aktivitetType\":\"IJOBB\"," +
                "\"aktivitetStatus\":\"FULLFORT\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2020-07-09T12:00:00+02:00",
                ISO_ZONED_DATE_TIME);

        ZonedDateTime zonedDateTime2 = ZonedDateTime.parse("2020-05-28T09:47:42.48+02:00",
                ISO_ZONED_DATE_TIME);

        KafkaAktivitetMelding aktivitetDataFraKafka = new KafkaAktivitetMelding()
                .setVersion(1L)
                .setAktivitetId("144136")
                .setAktorId("123456789")
                .setFraDato(zonedDateTime)
                .setTilDato(null)
                .setEndretDato(zonedDateTime2)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.FULLFORT)
                .setAvtalt(true)
                .setHistorisk(false);

        assertThat(fromJson(aktivitetKafkaMeldingMedVersion, KafkaAktivitetMelding.class)).isEqualTo(aktivitetDataFraKafka);
    }

    @Test
    public void skal_deserialisere_kafka_payload_V4() {
        String aktivitetKafkaMelding = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"version\":\"1\"," +
                "\"aktorId\":\"123456789\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                "\"endretDato\":\"2020-05-28T09:47:42.48+02:00\"," +
                "\"aktivitetType\":\"IJOBB\"," +
                "\"aktivitetStatus\":\"FULLFORT\"," +
                "\"endringsType\":\"OPPRETTET\"," +
                "\"lagtInnAv\":\"NAV\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";

        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2020-07-09T12:00:00+02:00",
                ISO_ZONED_DATE_TIME);

        ZonedDateTime zonedDateTime2 = ZonedDateTime.parse("2020-05-28T09:47:42.48+02:00",
                ISO_ZONED_DATE_TIME);

        KafkaAktivitetMelding aktivitetDataFraKafka = new KafkaAktivitetMelding()
                .setAktivitetId("144136")
                .setAktorId("123456789")
                .setVersion(1L)
                .setFraDato(zonedDateTime)
                .setTilDato(null)
                .setEndretDato(zonedDateTime2)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.FULLFORT)
                .setLagtInnAv(KafkaAktivitetMelding.InnsenderData.NAV)
                .setEndringsType(KafkaAktivitetMelding.EndringsType.OPPRETTET)
                .setAvtalt(true)
                .setHistorisk(false);

        assertThat(fromJson(aktivitetKafkaMelding, KafkaAktivitetMelding.class)).isEqualTo(aktivitetDataFraKafka);
    }

}
