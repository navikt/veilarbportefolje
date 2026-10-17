package no.nav.pto.veilarbportefolje.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.pto.veilarbportefolje.oppfolging.dto.ManuellStatusDTO
import no.nav.pto.veilarbportefolje.oppfolging.dto.NyForVeilederDTO
import no.nav.pto.veilarbportefolje.skjerming.SkjermingDTO
import no.nav.pto.veilarbportefolje.util.objectMapper
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO as YtelserKafkaDTOKlasse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream

/**
 * Kontrakttest for Kafka-meldinger: verifiserer at DTO-er kan deserialiseres
 * fra JSON-fixtures uten NPE, og at ugyldige meldinger gir feil.
 *
 * Fil-basert @ParameterizedTest — nye testcases legges til ved å
 * droppe JSON-filer i tilhørende katalog under src/test/resources/kafka/.
 */
class KafkaMessageKontraktTest {

    @Nested
    inner class YtelserKafkaMelding {

        @ParameterizedTest(name = "{0}")
        @MethodSource("no.nav.pto.veilarbportefolje.kafka.KafkaMessageKontraktTest#gyldigeYtelseMeldinger")
        fun `skal deserialisere gyldig ytelse-melding`(filnavn: String, json: String) {
            val dto = objectMapper.readValue(json, YtelserKafkaDTOKlasse::class.java)

            assertThat(dto.personId).isNotBlank()
            assertThat(dto.meldingstype).isIn(*YTELSE_MELDINGSTYPE.entries.toTypedArray())
            assertThat(dto.ytelsestype).isIn(*YTELSE_TYPE.entries.toTypedArray())
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("no.nav.pto.veilarbportefolje.kafka.KafkaMessageKontraktTest#ugyldigeYtelseMeldinger")
        fun `skal feile deserialisering ved ugyldig ytelse-melding`(filnavn: String, json: String) {
            assertThatThrownBy {
                objectMapper.readValue(json, YtelserKafkaDTOKlasse::class.java)
            }.isInstanceOf(RuntimeException::class.java)
        }
    }

    @Nested
    inner class ManuellStatusMelding {

        @ParameterizedTest(name = "{0}")
        @MethodSource("no.nav.pto.veilarbportefolje.kafka.KafkaMessageKontraktTest#gyldigeManuellStatusMeldinger")
        fun `skal deserialisere manuell-status melding`(filnavn: String, json: String) {
            val dto = ObjectMapper().readValue(json, ManuellStatusDTO::class.java)

            assertThat(dto.aktorId).isNotBlank()
        }
    }

    @Nested
    inner class NyForVeilederMelding {

        @ParameterizedTest(name = "{0}")
        @MethodSource("no.nav.pto.veilarbportefolje.kafka.KafkaMessageKontraktTest#gyldigeNyForVeilederMeldinger")
        fun `skal deserialisere ny-for-veileder melding`(filnavn: String, json: String) {
            val dto = objectMapper.readValue(json, NyForVeilederDTO::class.java)

            assertThat(dto.aktorId).isNotNull()
        }
    }

    @Nested
    inner class SkjermingMelding {

        @ParameterizedTest(name = "{0}")
        @MethodSource("no.nav.pto.veilarbportefolje.kafka.KafkaMessageKontraktTest#gyldigeSkjermingMeldinger")
        fun `skal deserialisere skjerming-melding`(filnavn: String, json: String) {
            val dto = ObjectMapper().readValue(json, SkjermingDTO::class.java)

            assertThat(dto.skjermetFra).isNotNull()
            assertThat(dto.skjermetFra).hasSize(6)
        }
    }

    @Nested
    inner class KafkaMeldingMedMetadataKontrakt {

        @Test
        fun `tilKafkaMeldingMedMetadata kaster exception ved null value (tombstone-melding)`() {
            val record = ConsumerRecord<String, String?>("topic", 0, 0L, "key", null)
            assertThatThrownBy { record.tilKafkaMeldingMedMetadata() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("mangler value")
        }

        @Test
        fun `tilKafkaMeldingMedMetadata bevarer metadata fra ConsumerRecord`() {
            val record = ConsumerRecord<String, String>("topic", 2, 42L, "min-nøkkel", "verdi")
            val melding = record.tilKafkaMeldingMedMetadata()

            assertThat(melding.value).isEqualTo("verdi")
            assertThat(melding.metadata.partition).isEqualTo(2)
            assertThat(melding.metadata.offset).isEqualTo(42L)
            assertThat(melding.metadata.key).isEqualTo("min-nøkkel")
        }
    }

    companion object {
        @JvmStatic
        fun gyldigeYtelseMeldinger(): Stream<Arguments> =
            lastJsonFilerFraKatalog("/kafka/ytelserkafka/gyldig")

        @JvmStatic
        fun ugyldigeYtelseMeldinger(): Stream<Arguments> =
            lastJsonFilerFraKatalog("/kafka/ytelserkafka/ugyldig")

        @JvmStatic
        fun gyldigeManuellStatusMeldinger(): Stream<Arguments> =
            lastJsonFilerFraKatalog("/kafka/manuell-status/gyldig")

        @JvmStatic
        fun gyldigeNyForVeilederMeldinger(): Stream<Arguments> =
            lastJsonFilerFraKatalog("/kafka/ny-for-veileder/gyldig")

        @JvmStatic
        fun gyldigeSkjermingMeldinger(): Stream<Arguments> =
            lastJsonFilerFraKatalog("/kafka/skjerming/gyldig")

        private fun lastJsonFilerFraKatalog(katalogsti: String): Stream<Arguments> {
            val uri: URI = KafkaMessageKontraktTest::class.java.getResource(katalogsti)?.toURI()
                ?: return Stream.empty()
            return Files.walk(Paths.get(uri), 1)
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                .sorted()
                .map { fil -> Arguments.of(fil.fileName.toString(), Files.readString(fil)) }
        }
    }
}
