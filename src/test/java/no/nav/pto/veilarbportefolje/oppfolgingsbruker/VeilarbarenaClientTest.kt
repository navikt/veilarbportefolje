package no.nav.pto.veilarbportefolje.oppfolgingsbruker

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.rest.client.RestClient
import no.nav.common.types.identer.Fnr
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Supplier

class VeilarbarenaClientTest {

    @JvmField
    @Rule
    val wireMockRule: WireMockRule = WireMockRule(0)

    @Test
    fun hentOppfolgingsbruker_gir_forventet_respons_naar_bruker_eksisterer() {
        val fnr = Fnr.of("123")

        val client = VeilarbarenaClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        val responseBody = """
                    {
                      "fodselsnr": "17858998980",
                      "formidlingsgruppekode": "ARBS",
                      "iservFraDato": "2024-04-04T00:00:00+02:00",
                      "navKontor": "0220",
                      "kvalifiseringsgruppekode": "BATT",
                      "rettighetsgruppekode": "INDS",
                      "hovedmaalkode": "SKAFFEA",
                      "sikkerhetstiltakTypeKode": "TFUS",
                      "frKode": "6",
                      "harOppfolgingssak": true,
                      "sperretAnsatt": false,
                      "erDoed": false,
                      "doedFraDato": null,
                      "sistEndretDato": "2024-04-04T00:00:00+02:00"
                    }
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/hent-oppfolgingsbruker")).withRequestBody(
                WireMock.equalToJson(
                    "{\"fnr\":\"$fnr\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response = client.hentOppfolgingsbruker(fnr)

        val forventet = OppfolgingsbrukerDTO(
            fodselsnr = "17858998980",
            formidlingsgruppekode = "ARBS",
            navKontor = "0220",
            iservFraDato = ZonedDateTime.parse("2024-04-04T00:00:00+02:00"),
            kvalifiseringsgruppekode = "BATT",
            rettighetsgruppekode = "INDS",
            hovedmaalkode = "SKAFFEA",
            sikkerhetstiltakTypeKode = "TFUS",
            frKode = "6",
            harOppfolgingssak = true,
            sperretAnsatt = false,
            erDoed = false,
            doedFraDato = null,
            sistEndretDato = ZonedDateTime.parse("2024-04-04T00:00:00+02:00")
        )

        Assertions.assertThat(response).isEqualTo(Optional.of(forventet))
    }

    @Test
    fun hentOppfolgingsbruker_gir_forventet_respons_naar_bruker_ikke_eksisterer() {
        val fnr = Fnr.of("123")

        val client = VeilarbarenaClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/hent-oppfolgingsbruker")).withRequestBody(
                WireMock.equalToJson(
                    "{\"fnr\":\"$fnr\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(404))
        )

        val response = client.hentOppfolgingsbruker(fnr)

        Assertions.assertThat(response).isEqualTo(Optional.empty<OppfolgingsbrukerDTO>())
    }

    @Test
    fun hentOppfolgingsbruker_gir_forventet_respons_naar_downstream_server_har_feil() {
        val fnr = Fnr.of("123")

        val client = VeilarbarenaClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/hent-oppfolgingsbruker")).withRequestBody(
                WireMock.equalToJson(
                    "{\"fnr\":\"$fnr\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(500))
        )

        try {
            client.hentOppfolgingsbruker(fnr)
        } catch (e: Exception) {
            Assertions.assertThat(e).isInstanceOf(RuntimeException::class.java)
        }
    }
}