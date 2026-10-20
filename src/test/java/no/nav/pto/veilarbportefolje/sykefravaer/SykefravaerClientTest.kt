package no.nav.pto.veilarbportefolje.sykefravaer

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfelleDTO
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfellePersonDTO
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

@WireMockTest
class SykefravaerClientTest {

    private val fnr = "10108000000"
    private val path = "/api/system/v1/oppfolgingstilfelle/personident"

    private fun client(wireMockRuntimeInfo: WireMockRuntimeInfo) = SykefravaerClient(
        "http://localhost:" + wireMockRuntimeInfo.httpPort,
        { "TOKEN" }
    )

    @Test
    fun `hentSykefravaersoppfolingPeriode gir forventet respons naar bruker har oppfolgingstilfeller`(
        wireMockRuntimeInfo: WireMockRuntimeInfo
    ) {
        val responseBody = """
            {
              "oppfolgingstilfelleList": [
                {
                  "arbeidstakerAtTilfelleEnd": true,
                  "start": "2026-09-01",
                  "end": "2026-11-30",
                  "antallSykedager": 91,
                  "varighetUker": 7,
                  "virksomhetsnummerList": ["987654321"]
                },
                {
                  "arbeidstakerAtTilfelleEnd": false,
                  "start": "2025-01-01",
                  "end": "2025-02-01",
                  "antallSykedager": 32,
                  "varighetUker": 4,
                  "virksomhetsnummerList": []
                }
              ],
              "personIdent": "$fnr",
              "dodsdato": null,
              "hasGjentakendeSykefravar": false
            }
        """.trimIndent()

        WireMock.givenThat(
            WireMock.get(WireMock.urlEqualTo(path))
                .withHeader("Authorization", WireMock.equalTo("Bearer TOKEN"))
                .withHeader(NAV_PERSONIDENT_HEADER, WireMock.equalTo(fnr))
                .willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client(wireMockRuntimeInfo).hentSykefravaersoppfolingPeriode(fnr)

        val forventet = OppfolgingstilfellePersonDTO(
            oppfolgingstilfelleList = listOf(
                OppfolgingstilfelleDTO(
                    arbeidstakerAtTilfelleEnd = true,
                    start = LocalDate.of(2026, 9, 1),
                    end = LocalDate.of(2026, 11, 30),
                ),
                OppfolgingstilfelleDTO(
                    arbeidstakerAtTilfelleEnd = false,
                    start = LocalDate.of(2025, 1, 1),
                    end = LocalDate.of(2025, 2, 1),
                ),
            ),
            personIdent = fnr,
        )
        assertThat(response).isEqualTo(forventet)
    }

    @Test
    fun `hentSykefravaersoppfolingPeriode gir tom liste naar bruker ikke har oppfolgingstilfeller`(
        wireMockRuntimeInfo: WireMockRuntimeInfo
    ) {
        val responseBody = """
            {
              "oppfolgingstilfelleList": [],
              "personIdent": "$fnr",
              "dodsdato": null,
              "hasGjentakendeSykefravar": false
            }
        """.trimIndent()

        WireMock.givenThat(
            WireMock.get(WireMock.urlEqualTo(path))
                .withHeader(NAV_PERSONIDENT_HEADER, WireMock.equalTo(fnr))
                .willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client(wireMockRuntimeInfo).hentSykefravaersoppfolingPeriode(fnr)

        assertThat(response.oppfolgingstilfelleList).isEmpty()
        assertThat(response.personIdent).isEqualTo(fnr)
    }

    @Test
    fun `hentSykefravaersoppfolingPeriode kaster exception ved feilrespons`(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        WireMock.givenThat(
            WireMock.get(WireMock.urlEqualTo(path))
                .willReturn(WireMock.aResponse().withStatus(500))
        )

        assertThatThrownBy { client(wireMockRuntimeInfo).hentSykefravaersoppfolingPeriode(fnr) }
            .isInstanceOf(RuntimeException::class.java)
    }
}
