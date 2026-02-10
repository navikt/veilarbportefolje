package no.nav.pto.veilarbportefolje.dagpenger

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerBeregningerResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPeriodeDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPerioderResponseDto
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class DagpengerClientTest {

    @JvmField
    @Rule
    val wireMockRule: WireMockRule = WireMockRule(0)


    @Test
    fun hentDagpengerPerioder_gir_forventet_respons_naar_bruker_eksisterer() {
        val fnr = Fnr.of("12345678901")
        val client = DagpengerClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" }
        )

        val responseBody = """
                    {
                      "personIdent": "12345678901",
                      "perioder": [
                        {
                          "fraOgMedDato": "2026-01-20",
                          "tilOgMedDato": "2026-01-30",
                          "ytelseType": "DAGPENGER_ARBEIDSSOKER_ORDINAER",
                          "kilde": "DP_SAK"
                        }
                      ]
                    }
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/dagpenger/datadeling/v1/perioder")).withRequestBody(
                WireMock.equalToJson(
                    "{\"personIdent\":\"$fnr\", \"fraOgMedDato\":\"2024-01-01\", \"tilOgMedDato\":\"2026-12-31\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentDagpengerPerioder(fnr.get(), "2024-01-01", "2026-12-31")
        val forventet = DagpengerPerioderResponseDto(
            personIdent = "12345678901",
            perioder = listOf(
                DagpengerPeriodeDto(
                    fraOgMedDato = LocalDate.of(2026, 1, 20),
                    tilOgMedDato = LocalDate.of(2026, 1, 30),
                    ytelseType = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                    kilde = "DP_SAK"
                )
            )
        )
        Assertions.assertThat(response).isEqualTo(forventet)

    }


    @Test
    fun hentDagpengerBeregninger_gir_forventet_respons_naar_bruker_eksisterer() {
        val fnr = Fnr.of("12345678901")
        val client = DagpengerClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" }
        )

        val responseBody = """
                    [
                      {
                        "fraOgMed": "2026-01-20",
                        "sats": 686,
                        "utbetaltBeløp": 457,
                        "gjenståendeDager": 519
                      },
                      {
                        "fraOgMed": "2026-01-21",
                        "sats": 686,
                        "utbetaltBeløp": 457,
                        "gjenståendeDager": 518
                      },
                      {
                        "fraOgMed": "2026-01-22",
                        "sats": 686,
                        "utbetaltBeløp": 457,
                        "gjenståendeDager": 517
                      }
                    ]
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/dagpenger/datadeling/v1/beregninger")).withRequestBody(
                WireMock.equalToJson(
                    "{\"personIdent\":\"$fnr\", \"fraOgMedDato\":\"2024-01-01\", \"tilOgMedDato\":\"2026-12-31\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentDagpengerBeregninger(fnr.get(), "2024-01-01", "2026-12-31")
        val forventet = listOf(DagpengerBeregningerResponseDto(
            fraOgMed = LocalDate.of(2026, 1, 20),
            sats = 686,
            utbetaltBeløp = 457,
            gjenståendeDager = 519
        ),
            DagpengerBeregningerResponseDto(
                fraOgMed = LocalDate.of(2026, 1, 21),
                sats = 686,
                utbetaltBeløp = 457,
                gjenståendeDager = 518
            ),
            DagpengerBeregningerResponseDto(
                fraOgMed = LocalDate.of(2026, 1, 22),
                sats = 686,
                utbetaltBeløp = 457,
                gjenståendeDager = 517
            )
        )
        Assertions.assertThat(response).isEqualTo(forventet)
    }
}
