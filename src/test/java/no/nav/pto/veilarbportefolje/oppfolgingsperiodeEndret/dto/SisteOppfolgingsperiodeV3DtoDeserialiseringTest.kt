package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import com.fasterxml.jackson.databind.JsonMappingException
import no.nav.pto.veilarbportefolje.util.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class SisteOppfolgingsperiodeV3DtoDeserialiseringTest {

    private fun deserialize(json: String): SisteOppfolgingsperiodeV3Dto {
        return objectMapper.readValue(json, SisteOppfolgingsperiodeV3Dto::class.java)
    }

    @Test
    fun `deserialiserer OPPFOLGING_STARTET til GjeldendeOppfolgingsperiodeV3Dto`() {
        val json = """
        {
            "oppfolgingsperiodeUuid": "11111111-1111-1111-1111-111111111111",
            "sisteEndringsType": "OPPFOLGING_STARTET",
            "aktorId": "123456789",
            "ident": "01010112345",
            "startTidspunkt": "2025-01-01T12:00:00+01:00",
            "sluttTidspunkt": null,
            "kontor": { "kontorNavn": "Nav Test", "kontorId": "1234" },
            "producerTimestamp": "2025-01-01T12:00:00+01:00"
        }
        """.trimIndent()

        val result = deserialize(json)

        assertThat(result).isInstanceOf(GjeldendeOppfolgingsperiodeV3Dto::class.java)
        val gjeldende = result as GjeldendeOppfolgingsperiodeV3Dto
        assertThat(gjeldende.aktorId).isEqualTo("123456789")
        assertThat(gjeldende.ident).isEqualTo("01010112345")
        assertThat(gjeldende.kontor!!.kontorId).isEqualTo("1234")
        assertThat(gjeldende.kontor.kontorNavn).isEqualTo("Nav Test")
        assertThat(gjeldende.sluttTidspunkt).isNull()
    }

    @Test
    fun `deserialiserer ARBEIDSOPPFOLGINGSKONTOR_ENDRET til GjeldendeOppfolgingsperiodeV3Dto`() {
        val json = """
        {
            "oppfolgingsperiodeUuid": "11111111-1111-1111-1111-111111111111",
            "sisteEndringsType": "ARBEIDSOPPFOLGINGSKONTOR_ENDRET",
            "aktorId": "123456789",
            "ident": "01010112345",
            "startTidspunkt": "2025-01-01T12:00:00+01:00",
            "sluttTidspunkt": null,
            "kontor": { "kontorNavn": "Nav Annet", "kontorId": "5678" },
            "producerTimestamp": "2025-01-01T12:00:00+01:00"
        }
        """.trimIndent()

        val result = deserialize(json)

        assertThat(result).isInstanceOf(GjeldendeOppfolgingsperiodeV3Dto::class.java)
        val gjeldende = result as GjeldendeOppfolgingsperiodeV3Dto
        assertThat(gjeldende.sisteEndringsType).isEqualTo(SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET)
        assertThat(gjeldende.kontor!!.kontorId).isEqualTo("5678")
    }

    @Test
    fun `deserialiserer OPPFOLGING_AVSLUTTET til AvsluttetOppfolgingsperiodeV3`() {
        val json = """
        {
            "oppfolgingsperiodeUuid": "22222222-2222-2222-2222-222222222222",
            "sisteEndringsType": "OPPFOLGING_AVSLUTTET",
            "aktorId": "987654321",
            "ident": "01010154321",
            "startTidspunkt": "2025-01-01T12:00:00+01:00",
            "sluttTidspunkt": "2025-06-01T12:00:00+02:00",
            "kontor": null,
            "producerTimestamp": "2025-06-01T12:00:00+02:00"
        }
        """.trimIndent()

        val result = deserialize(json)

        assertThat(result).isInstanceOf(AvsluttetOppfolgingsperiodeV3Dto::class.java)
        val avsluttet = result as AvsluttetOppfolgingsperiodeV3Dto
        assertThat(avsluttet.aktorId).isEqualTo("987654321")
        assertThat(avsluttet.sluttTidspunkt).isNotNull()
        assertThat(avsluttet.kontor).isNull()
    }

    @Test
    fun `feiler ved ukjent sisteEndringsType`() {
        val json = """
        {
            "oppfolgingsperiodeUuid": "11111111-1111-1111-1111-111111111111",
            "sisteEndringsType": "UKJENT_TYPE",
            "aktorId": "123456789",
            "ident": "01010112345",
            "startTidspunkt": "2025-01-01T12:00:00+01:00",
            "sluttTidspunkt": null,
            "kontor": null,
            "producerTimestamp": "2025-01-01T12:00:00+01:00"
        }
        """.trimIndent()

        assertThatThrownBy { deserialize(json) }
            .isInstanceOf(JsonMappingException::class.java)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ugyldigePayloads")
    fun `feiler ved ugyldige payloads`(beskrivelse: String, json: String) {
        assertThatThrownBy { deserialize(json) }
            .describedAs(beskrivelse)
            .isInstanceOf(JsonMappingException::class.java)
    }

    companion object {
        @JvmStatic
        fun ugyldigePayloads(): Stream<Arguments> {
            //language=json
            val gyldigGjeldendePayload = """
            {
                "oppfolgingsperiodeUuid": "11111111-1111-1111-1111-111111111111",
                "sisteEndringsType": "OPPFOLGING_STARTET",
                "aktorId": "123456789",
                "ident": "01010112345",
                "startTidspunkt": "2025-01-01T12:00:00+01:00",
                "sluttTidspunkt": null,
                "kontor": { "kontorNavn": "Nav Test", "kontorId": "1234" },
                "producerTimestamp": "2025-01-01T12:00:00+01:00"
            }
            """.trimIndent()

            //language=json
            val gyldigAvsluttetPayload = """
            {
                "oppfolgingsperiodeUuid": "22222222-2222-2222-2222-222222222222",
                "sisteEndringsType": "OPPFOLGING_AVSLUTTET",
                "aktorId": "987654321",
                "ident": "01010154321",
                "startTidspunkt": "2025-01-01T12:00:00+01:00",
                "sluttTidspunkt": "2025-06-01T12:00:00+02:00",
                "kontor": null,
                "producerTimestamp": "2025-06-01T12:00:00+02:00"
            }
            """.trimIndent()

            return Stream.of(
                Arguments.of(
                    "null i oppfolgingsperiodeUuid",
                    gyldigGjeldendePayload.replace(
                        "\"oppfolgingsperiodeUuid\": \"11111111-1111-1111-1111-111111111111\"",
                        "\"oppfolgingsperiodeUuid\": null"
                    )
                ),
                Arguments.of(
                    "null i aktorId",
                    gyldigGjeldendePayload.replace(
                        "\"aktorId\": \"123456789\"",
                        "\"aktorId\": null"
                    )
                ),
                Arguments.of(
                    "mangler ident",
                    gyldigGjeldendePayload.replace(
                        "    \"ident\": \"01010112345\",\n",
                        ""
                    )
                ),
                Arguments.of(
                    "mangler kontor for gjeldende oppfolgingsperiode",
                    gyldigGjeldendePayload.replace(
                        "\"kontor\": { \"kontorNavn\": \"Nav Test\", \"kontorId\": \"1234\" }",
                        "\"kontor\": null"
                    )
                ),
                Arguments.of(
                    "mangler sluttTidspunkt for avsluttet oppfolgingsperiode",
                    gyldigAvsluttetPayload.replace(
                        "\"sluttTidspunkt\": \"2025-06-01T12:00:00+02:00\"",
                        "\"sluttTidspunkt\": null"
                    )
                )
            )
        }
    }
}
