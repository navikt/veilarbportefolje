package no.nav.pto.veilarbportefolje.ensligforsorger

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.json.JsonUtils.fromJson
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.ensligforsorger.client.EnsligForsorgerClientImpl
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.OvergangsstønadResponseDto
import no.nav.pto.veilarbportefolje.util.TestUtil.readTestResourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

@WireMockTest
class EnsligForsorgerClientImplTest {

    @Test
    fun hentEnsligForsorger_gir_forventet_respons_naar_bruker_eksisterer(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        val fnr = Fnr.of("12518904661")
        val ensligForsorgerJson = readTestResourceFile("ensligForsorgerApiData.json")
        val client = EnsligForsorgerClientImpl(
            "http://localhost:" + wireMockRuntimeInfo.httpPort
        ) { "TOKEN" }

        println("ensligForsorgerJson: $ensligForsorgerJson")

        val expected = fromJson(ensligForsorgerJson, OvergangsstønadResponseDto::class.java);

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/ekstern/perioder/perioder-aktivitet"))
                .withRequestBody(
                    WireMock.equalToJson(
                        "{\"personIdent\":\"${fnr.get()}\"}"
                    )
                )
                .willReturn(WireMock.aResponse().withStatus(200).withBody(ensligForsorgerJson))
        )
        val response: Optional<OvergangsstønadResponseDto> = client.hentEnsligForsorgerOvergangsstonad(fnr);

        assertThat(response.get().data.personIdent[0]).isEqualTo(expected.data.personIdent[0])
        assertThat(response.get()).isEqualTo(expected)
    }
}
