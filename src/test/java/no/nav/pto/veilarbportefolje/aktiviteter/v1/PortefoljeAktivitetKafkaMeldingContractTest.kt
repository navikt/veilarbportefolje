package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class PortefoljeAktivitetKafkaMeldingContractTest {
    private val deserializer = Deserializers.jsonDeserializer(PortefoljeAktivitetKafkaMelding::class.java)

    @Test
    fun `skal deserialisere gyldig v4-melding`() {
        val payload = """
            {
              "aktivitetId": "144136",
              "version": 1,
              "aktorId": "123456789",
              "fraDato": "2020-07-09T12:00:00+02:00",
              "tilDato": null,
              "endretDato": "2020-05-28T09:47:42.48+02:00",
              "aktivitetType": "STILLING_FRA_NAV",
              "aktivitetStatus": "FULLFORT",
              "endringsType": "OPPRETTET",
              "lagtInnAv": "NAV",
              "stillingFraNavData": {
                "cvKanDelesStatus": "IKKE_SVART",
                "svarfrist": "2022-08-18"
              },
              "avtalt": true,
              "historisk": false
            }
        """.trimIndent()

        val resultat = deserializer.deserialize("pto.portefolje-aktivitet-v1", payload.toByteArray())

        assertThat(resultat).isNotNull
        assertThat(resultat!!.aktivitetId).isEqualTo("144136")
        assertThat(resultat.version).isEqualTo(1L)
        assertThat(resultat.aktorId).isEqualTo("123456789")
        assertThat(resultat.fraDato).isEqualTo(date("2020-07-09T12:00:00+02:00"))
        assertThat(resultat.tilDato).isNull()
        assertThat(resultat.endretDato).isEqualTo(date("2020-05-28T09:47:42.48+02:00"))
        assertThat(resultat.aktivitetType).isEqualTo(PortefoljeAktivitetKafkaMelding.AktivitetTypeDTO.STILLING_FRA_NAV)
        assertThat(resultat.aktivitetStatus).isEqualTo(PortefoljeAktivitetKafkaMelding.AktivitetStatus.FULLFORT)
        assertThat(resultat.endringsType).isEqualTo(PortefoljeAktivitetKafkaMelding.EndringsType.OPPRETTET)
        assertThat(resultat.lagtInnAv).isEqualTo(PortefoljeAktivitetKafkaMelding.Innsender.NAV)
        assertThat(resultat.stillingFraNavData?.cvKanDelesStatus).isEqualTo(PortefoljeAktivitetKafkaMelding.CvKanDelesStatus.IKKE_SVART)
        assertThat(resultat.stillingFraNavData?.svarfrist?.toLocalDate()).isEqualTo(LocalDate.parse("2022-08-18"))
        assertThat(resultat.avtalt).isTrue()
        assertThat(resultat.historisk).isFalse()
    }

    @Test
    fun `skal ignorere ukjente felter og beholde tiltak as is`() {
        val payload = """
            {
              "aktivitetId": "200",
              "version": 2,
              "aktorId": "999",
              "fraDato": null,
              "tilDato": null,
              "endretDato": null,
              "aktivitetType": "TILTAK",
              "aktivitetStatus": "GJENNOMFORES",
              "endringsType": "REDIGERT",
              "lagtInnAv": "SYSTEM",
              "avtalt": false,
              "historisk": false,
              "tiltakskode": "ARBFORB",
              "ukjentFelt": "skal ignoreres"
            }
        """.trimIndent()

        val resultat = deserializer.deserialize("pto.portefolje-aktivitet-v1", payload.toByteArray())

        assertThat(resultat).isNotNull
        assertThat(resultat!!.aktivitetType).isEqualTo(PortefoljeAktivitetKafkaMelding.AktivitetTypeDTO.TILTAK)
        assertThat(resultat.tiltakskode).isEqualTo("ARBFORB")
        assertThat(resultat.lagtInnAv).isEqualTo(PortefoljeAktivitetKafkaMelding.Innsender.SYSTEM)
    }

    private fun date(value: String): Date = Date.from(ZonedDateTime.parse(value).toInstant())
}
