package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.pto.veilarbportefolje.aktviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.aktviteter.AktivitetTypeData;
import no.nav.pto.veilarbportefolje.aktviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.aktviteter.KafkaAktivitetService;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDataFraFeed;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.json.JsonUtils.fromJson;
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

        KafkaAktivitetMelding aktivitetDataFraKafka = KafkaAktivitetMelding.builder()
                .aktivitetId("144136")
                .aktorId("123456789")
                .fraDato(Timestamp.from(zonedDateTime.toInstant()))
                .tilDato(null)
                .endretDato(Timestamp.from(zonedDateTime2.toInstant()))
                .aktivitetType(AktivitetTypeData.IJOBB)
                .aktivitetStatus(AktivitetStatus.FULLFORT)
                .avtalt(true)
                .historisk(false)
                .build();

        assertThat(fromJson(aktivitetKafkaMelding, KafkaAktivitetMelding.class)).isEqualTo(aktivitetDataFraKafka);

    }

    @Test
    public void skal_deserialisere_aktivitetfeeddata_payload() {
        String aktivitetKafkaMelding = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"aktorId\":\"123456789\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                "\"endretDato\":\"2020-05-28T09:47:42.48+02:00\"," +
                "\"aktivitetType\":\"IJOBB\"," +
                "\"status\":\"FULLFORT\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";

        ZonedDateTime zonedDateTime = LocalDateTime.parse("2020-07-09T12:00:00+02:00",
                ISO_ZONED_DATE_TIME).atZone(ZoneId.of("Europe/Oslo"));

        ZonedDateTime zonedDateTime2 = LocalDateTime.parse("2020-05-28T09:47:42.48+02:00",
                ISO_ZONED_DATE_TIME).atZone(ZoneId.of("Europe/Oslo"));

        AktivitetDataFraFeed aktivitetDataFraFeed = new AktivitetDataFraFeed()
                .setAktivitetId("144136")
                .setAktorId("123456789")
                .setFraDato(Timestamp.from(zonedDateTime.toInstant()))
                .setTilDato(null)
                .setEndretDato(Timestamp.from(zonedDateTime2.toInstant()))
                .setAktivitetType(AktivitetTypeData.IJOBB.name())
                .setStatus(AktivitetStatus.FULLFORT.name())
                .setAvtalt(true)
                .setHistorisk(false);

        assertThat(fromJson(aktivitetKafkaMelding, AktivitetDataFraFeed.class)).isEqualTo(aktivitetDataFraFeed);

    }

    @Test
    public void skal_mappe_kafkaaktivitet_til_aktivitetdata() {

        ZonedDateTime fraDato = LocalDateTime.parse("2020-07-09T12:00:00+02:00",
                ISO_ZONED_DATE_TIME).atZone(ZoneId.of("Europe/Oslo"));

        ZonedDateTime tilDato = LocalDateTime.parse("2020-07-28T09:47:42.48+02:00",
                ISO_ZONED_DATE_TIME).atZone(ZoneId.of("Europe/Oslo"));

        ZonedDateTime endretDato = LocalDateTime.parse("2020-07-14T09:47:42.48+02:00",
                ISO_ZONED_DATE_TIME).atZone(ZoneId.of("Europe/Oslo"));

        KafkaAktivitetMelding aktivitetDataFraKafka = KafkaAktivitetMelding.builder()
                .aktivitetId("144136")
                .aktorId("123456789")
                .fraDato(Timestamp.from(fraDato.toInstant()))
                .tilDato(Timestamp.from(tilDato.toInstant()))
                .endretDato(Timestamp.from(endretDato.toInstant()))
                .aktivitetType(AktivitetTypeData.IJOBB)
                .aktivitetStatus(AktivitetStatus.FULLFORT)
                .avtalt(true)
                .historisk(false)
                .build();

        AktivitetDataFraFeed aktivitetDataFraFeed = new AktivitetDataFraFeed()
                .setAktivitetId("144136")
                .setAktorId("123456789")
                .setFraDato(Timestamp.from(fraDato.toInstant()))
                .setTilDato(Timestamp.from(tilDato.toInstant()))
                .setEndretDato(Timestamp.from(endretDato.toInstant()))
                .setAktivitetType(AktivitetTypeData.IJOBB.name())
                .setStatus(AktivitetStatus.FULLFORT.name())
                .setAvtalt(true)
                .setHistorisk(false);


        assertThat(KafkaAktivitetService.mapTilAktivitetDataFraFeed(aktivitetDataFraKafka)).isEqualTo(aktivitetDataFraFeed);

    }

}
