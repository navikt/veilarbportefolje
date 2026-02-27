package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import no.nav.pto.veilarbportefolje.util.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SisteOppfolgingsperiodeV2DtoDeserialiseringTest {

    private fun deserialize(json: String): SisteOppfolgingsperiodeV2Dto {
        return objectMapper.readValue(json, SisteOppfolgingsperiodeV2Dto::class.java)
    }

    @Test
    fun `deserialiserer OPPFOLGING_STARTET til GjeldendeOppfolgingsperiodeV2Dto`() {
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

        assertThat(result).isInstanceOf(GjeldendeOppfolgingsperiodeV2Dto::class.java)
        val gjeldende = result as GjeldendeOppfolgingsperiodeV2Dto
        assertThat(gjeldende.aktorId).isEqualTo("123456789")
        assertThat(gjeldende.ident).isEqualTo("01010112345")
        assertThat(gjeldende.kontor!!.kontorId).isEqualTo("1234")
        assertThat(gjeldende.kontor.kontorNavn).isEqualTo("Nav Test")
        assertThat(gjeldende.sluttTidspunkt).isNull()
    }

    @Test
    fun `deserialiserer ARBEIDSOPPFOLGINGSKONTOR_ENDRET til GjeldendeOppfolgingsperiodeV2Dto`() {
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

        assertThat(result).isInstanceOf(GjeldendeOppfolgingsperiodeV2Dto::class.java)
        val gjeldende = result as GjeldendeOppfolgingsperiodeV2Dto
        assertThat(gjeldende.sisteEndringsType).isEqualTo(SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET)
        assertThat(gjeldende.kontor!!.kontorId).isEqualTo("5678")
    }

    @Test
    fun `deserialiserer OPPFOLGING_AVSLUTTET til AvsluttetOppfolgingsperiodeV2`() {
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

        assertThat(result).isInstanceOf(AvsluttetOppfolgingsperiodeV2Dto::class.java)
        val avsluttet = result as AvsluttetOppfolgingsperiodeV2Dto
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
            .isInstanceOf(Exception::class.java)
    }
}
