package no.nav.pto.veilarbportefolje.aap

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AapClientTest {

    @JvmField
    @Rule
    val wireMockRule: WireMockRule = WireMockRule(0)

    @Test
    fun hentAapForBruker_gir_forventet_respons_naar_bruker_eksisterer() {
        val fnr = Fnr.of("123")

        val client = AapClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" }
        )

        val responseBody = """
                    {"vedtak": [
                        {
                          "status": "LØPENDE",
                          "saksnummer": "4N7Y3Uo",
                          "periode": {
                            "fraOgMedDato": "2025-04-22",
                            "tilOgMedDato": "2026-04-21"
                          },
                          "rettighetsType": "BISTANDSBEHOV",
                          "kildesystem": "KELVIN",
                          "opphorsAarsak": null
                        }
                      ]
                    }
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/kelvin/maksimumUtenUtbetaling")).withRequestBody(
                WireMock.equalToJson(
                    "{\"personidentifikator\":\"$fnr\", \"fraOgMedDato\":\"2024-01-01\", \"tilOgMedDato\":\"2026-12-31\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentAapVedtak(fnr.get(), "2024-01-01", "2026-12-31")

        val forventet = AapVedtakResponseDto(
            vedtak = listOf(
                AapVedtakResponseDto.Vedtak(
                    status = "LØPENDE",
                    saksnummer = "4N7Y3Uo",
                    periode = AapVedtakResponseDto.Periode(
                        fraOgMedDato = LocalDate.parse("2025-04-22"),
                        tilOgMedDato = LocalDate.parse("2026-04-21")
                    ),
                    rettighetsType = "BISTANDSBEHOV",
                    kildesystem = "KELVIN",
                    opphorsAarsak = null
                )
            )
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }

}
