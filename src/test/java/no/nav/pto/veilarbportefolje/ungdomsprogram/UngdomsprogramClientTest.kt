package no.nav.pto.veilarbportefolje.ungdomsprogram

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Deltakelse
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.Periode
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.UngdomsprogramResponseDto
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class UngdomsprogramClientTest {

    @JvmField
    @Rule
    val wireMockRule: WireMockRule = WireMockRule(0)

    @Test
    fun hentAlleMedUngdomsprogram_gir_forventet_respons_naar_bruker_eksisterer() {
        val fnr = Fnr.of("123")

        val client = UngdomsprogramClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" }
        )

        val responseBody = """
            {
              "deltakelser": [
                {
                  "deltakerIdent": "31479936512",
                  "periode": {
                    "fraOgMed": "2025-05-01",
                    "tilOgMed": null,
                    "harForlengetPeriode": false,
                    "periodeMaksDato": "2026-04-29"
                  }
                },
                {
                  "deltakerIdent": "23469946401",
                  "periode": {
                    "fraOgMed": "2025-07-01",
                    "tilOgMed": null,
                    "harForlengetPeriode": false,
                    "periodeMaksDato": "2026-06-29"
                  }
                }
            ]}""".trimIndent()

        WireMock.givenThat(
            WireMock.get(WireMock.urlEqualTo("/ekstern/deltakelse/alle"))
                .willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentAlleMedUngdomsprogram()

        val forventet = UngdomsprogramResponseDto(
            deltakelser = listOf(
                Deltakelse(
                    deltakerIdent = "31479936512",
                    periode = Periode(
                        fraOgMed = LocalDate.parse("2025-05-01"),
                        tilOgMed = null,
                        harForlengetPeriode = false,
                        periodeMaksDato = LocalDate.parse("2026-04-29")
                    )
                ),
                Deltakelse(
                    deltakerIdent = "23469946401",
                    periode = Periode(
                        fraOgMed = LocalDate.parse("2025-07-01"),
                        tilOgMed = null,
                        harForlengetPeriode = false,
                        periodeMaksDato = LocalDate.parse("2026-06-29")
                    )
                )
            )
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }

}
