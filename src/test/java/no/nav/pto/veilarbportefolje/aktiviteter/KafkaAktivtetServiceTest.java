package no.nav.pto.veilarbportefolje.aktiviteter;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.common.json.JsonUtils.fromJson;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class KafkaAktivtetServiceTest {
    @Test
    public void skal_deserialisere_kafka_payload() {
        String aktivitetKafkaMelding = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"aktorId\":\"123456789\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                "\"endretDato\":\"2020-05-28T09:47:42.48+02:00\"," +
                "\"aktivitetType\":\"IJOBB\"," +
                "\"aktivitetStatus\":\"FULLFORT\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";

        ZonedDateTime zonedDateTime = LocalDateTime.parse("2020-07-09T12:00:00+02:00",
                ISO_ZONED_DATE_TIME).atZone(ZoneId.of("Europe/Oslo"));

        ZonedDateTime zonedDateTime2 = LocalDateTime.parse("2020-05-28T09:47:42.48+02:00",
                ISO_ZONED_DATE_TIME).atZone(ZoneId.of("Europe/Oslo"));

        KafkaAktivitetMelding aktivitetDataFraKafka = new KafkaAktivitetMelding()
                .setAktivitetId("144136")
                .setAktorId("123456789")
                .setFraDato(Timestamp.from(zonedDateTime.toInstant()))
                .setTilDato(null)
                .setEndretDato(Timestamp.from(zonedDateTime2.toInstant()))
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.IJOBB)
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.FULLFORT)
                .setAvtalt(true)
                .setHistorisk(false);

        assertThat(fromJson(aktivitetKafkaMelding, KafkaAktivitetMelding.class)).isEqualTo(aktivitetDataFraKafka);

    }

}
