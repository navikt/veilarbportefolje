package no.nav.pto.veilarbportefolje.ensligforsorger

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.util.TestUtil.readTestResourceFile
import no.nav.pto.veilarbportefolje.ensligforsorger.client.EnsligForsorgerClientImpl
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test

class EnsligForsorgerClientImplTest {

    @JvmField
    @Rule
    val wireMockRule: WireMockRule = WireMockRule(0)

    @Test
    fun hentEnsligForsorger_gir_forventet_respons_naar_bruker_eksisterer() {
        val fnr = Fnr.of("12345678901")
        val ensligForsorgerJson = readTestResourceFile("ensligForsorgerApiData.json")
        val client = EnsligForsorgerClientImpl(
            "http://localhost:" + wireMockRule.port()
        ) { "TOKEN" }
        println(client)
        println(ensligForsorgerJson)

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/ekstern/perioder/perioder-aktivitet")).withRequestBody(
                WireMock.equalToJson(
                    "{\"fnr\":\"$fnr\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(ensligForsorgerJson))
        )
       val response = client.hentEnsligForsorgerOvergangsstonad(fnr);

        Assertions.assertThat(response).isEqualTo(ensligForsorgerJson)
    }
}