package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.rest.client.RestClient
import no.nav.common.types.identer.Fnr
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.util.*

class OppslagArbeidssoekerregisteretClientTest {
    @JvmField
    @Rule
    val wireMockRule: WireMockRule = WireMockRule(0)

    @Test
    fun hentArbeidssoekerperioder_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode() {
        val fnr = Fnr.of("123")

        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        val responseBody = """
                    [
                      {
                        "periodeId": "ea0ad984-8b99-4fff-afd6-07737ab19d16",
                        "startet": {
                          "tidspunkt": "2024-04-23T13:04:40.739Z",
                          "utfoertAv": {
                            "type": "SLUTTBRUKER"
                          },
                          "kilde": "europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssokerregisteret-api-inngang:24.04.23.118-1",
                          "aarsak": "Er over 18 år, er bosatt i Norge i hendhold Folkeregisterloven"
                        },
                        "avsluttet": null
                      }
                    ]
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v1/veileder/arbeidssoekerperioder")).withRequestBody(
                WireMock.equalToJson(
                    "{\"identitetsnummer\":\"${fnr.get()}\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentArbeidssokerPerioder(fnr.get())

        val forventet = ArbeidssokerperiodeResponse(
            periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
            metadata = MetadataResponse(
                tidspunkt = LocalDateTime.parse("2024-04-23T13:04:40.739Z"),
                brukerResponse = BrukerResponse(
                    type = BrukerType.SLUTTBRUKER
                ),
                kilde = "europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssokerregisteret-api-inngang",
                aarsak = "Er over 18 år, er bosatt i Norge i hendhold Folkeregisterloven"
            ),
            avsluttet = null
        )

        Assertions.assertThat(response).isEqualTo(Optional.of(forventet))
    }
}