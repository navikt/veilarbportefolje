package no.nav.pto.veilarbportefolje.tiltakspenger

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.types.identer.Fnr
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test

class TiltakspengerClientTest {

    @JvmField
    @Rule
    val wireMockRule: WireMockRule = WireMockRule(0)

    @Test
    fun hentTiltakspengerForBruker_gir_forventet_respons_naar_bruker_eksisterer() {
        val fnr = Fnr.of("123")

        val client = TiltakspengerClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" }
        )

        val responseBody = """
                    [
                      {
                        "fom": "2024-10-02",
                        "tom": "2025-10-02",
                        "rettighet": "TILTAKSPENGER",
                        "vedtakId": "string",
                        "sakId": "123",
                        "saksnummer": "string",
                        "kilde": "tiltakspengerkilde",
                        "sats": 1073741824,
                        "satsBarnetillegg": 1073741824
                      }
                    ]
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/vedtak/detaljer")).withRequestBody(
                WireMock.equalToJson(
                    "{\"ident\":\"$fnr\", \"fom\":\"2024-01-01\", \"tom\":\"2026-12-31\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentTiltakspenger(fnr.get(), "2024-01-01", "2026-12-31")

        val forventet = listOf(
            TiltakspengerResponseDto(
                fom = "2024-10-02",
                tom = "2025-10-02",
                rettighet = TiltakspengerRettighet.TILTAKSPENGER,
                sakId = "123",
                kilde = "tiltakspengerkilde"
            )
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }

}
