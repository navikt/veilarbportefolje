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
                        "virkningsdato": "2024-10-02",
                        "uføregrad": 50
                      }
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/xxx/yyy")).withRequestBody(
                WireMock.equalToJson(
                    "{\"fnr\":\"$fnr\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentUforetrygd(fnr.get())

        val forventet = UforetrygdResponseDto(
            virkningsdato = LocalDate.of(2024, 10, 2),
            uføregrad = 50
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }
}

