package no.nav.pto.veilarbportefolje.uforetrygd

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

@WireMockTest
class UforetrygdClientTest {

    @Test
    fun hentUforetrygdForBruker_gir_forventet_respons_naar_bruker_eksisterer(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        val fnr = Fnr.of("123")

        val client = UforetrygdClient(
            "http://localhost:" + wireMockRuntimeInfo.httpPort,
            { "TOKEN" }
        )

        val responseBody = """
                      {
                      "lopendeUforetrygd": true,
                      "uforegrad": 50,
                      "forsteVirkningstidspunkt": "2024-10-02"
                      }
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/uforetrygd/ekstern/modia/vedtak")).withRequestBody(
                WireMock.equalToJson(
                    "{\"fnr\":\"$fnr\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentUforetrygd(fnr.get())

        val forventet = UforetrygdResponseDto(
            lopendeUforetrygd = true,
            forsteVirkningstidspunkt = LocalDate.of(2024, 10, 2),
            uforegrad = 50
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }
}

