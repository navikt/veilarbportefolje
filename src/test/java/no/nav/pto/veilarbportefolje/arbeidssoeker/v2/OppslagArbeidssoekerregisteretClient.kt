package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.common.rest.client.RestClient
import no.nav.common.types.identer.Fnr
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime
import java.util.*

class OppslagArbeidssoekerregisteretClientTest {
    @JvmField
    @Rule
    val wireMockRule: WireMockRule = WireMockRule(0)

    @Test
    fun hentArbeidssoekerperioder_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode() {
        val fnr = Fnr.of("123")

        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        val responseBody = """
                    [
                      {
                        "periodeId": "ea0ad984-8b99-4fff-afd6-07737ab19d16",
                        "startet": {
                          "tidspunkt": "2024-04-23T13:04:40.739Z",
                          "utfoertAv": {
                            "type": "SLUTTBRUKER"
                          },
                          "kilde": "europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssokerregisteret-api-inngang:24.04.23.118-1",
                          "aarsak": "Er over 18 år, er bosatt i Norge i hendhold Folkeregisterloven"
                        },
                        "avsluttet": null
                      }
                    ]
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v1/veileder/arbeidssoekerperioder")).withRequestBody(
                WireMock.equalToJson(
                    "{\"identitetsnummer\":\"${fnr.get()}\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response: List<ArbeidssokerperiodeResponse>? = client.hentArbeidssokerPerioder(fnr.get())

        val forventet: List<ArbeidssokerperiodeResponse>? = listOf(
            ArbeidssokerperiodeResponse(
                periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
                startet = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:04:40.739Z"),
                    utfoertAv = BrukerResponse(
                        type = BrukerType.SLUTTBRUKER
                    ),
                    kilde = "europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssokerregisteret-api-inngang:24.04.23.118-1",
                    aarsak = "Er over 18 år, er bosatt i Norge i hendhold Folkeregisterloven"
                ),
                avsluttet = null
            )
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }

    @Test
    fun hentOpplysningerOmArbeidssoeker_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode_og_har_registrert_opplysninger() {
        val fnr = Fnr.of("123")

        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        val responseBody = """
                    [
                      {
                        "opplysningerOmArbeidssoekerId": "913161a3-dde9-4448-abf8-2a01a043f8cd",
                        "periodeId": "ea0ad984-8b99-4fff-afd6-07737ab19d16",
                        "sendtInnAv": {
                          "tidspunkt": "2024-04-23T13:22:58.089Z",
                          "utfoertAv": {
                            "type": "SLUTTBRUKER"
                          },
                          "kilde": "paw-arbeidssoekerregisteret-inngang",
                          "aarsak": "opplysning om arbeidssøker sendt inn"
                        },
                        "jobbsituasjon": [
                          {
                            "beskrivelse": "ALDRI_HATT_JOBB",
                            "detaljer": {}
                          }
                        ],
                        "utdanning": {
                          "nus": "3",
                          "bestaatt": "JA",
                          "godkjent": "JA"
                        },
                        "helse": {
                          "helsetilstandHindrerArbeid": "NEI"
                        },
                        "annet": {
                          "andreForholdHindrerArbeid": "NEI"
                        }
                      }
                    ]
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v1/veileder/opplysninger-om-arbeidssoeker")).withRequestBody(
                WireMock.equalToJson(
                    "{\"identitetsnummer\":\"${fnr.get()}\",\"periodeId\":\"ea0ad984-8b99-4fff-afd6-07737ab19d16\"}"
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response: List<OpplysningerOmArbeidssoekerResponse>? = client.hentOpplysningerOmArbeidssoeker(fnr.get(), UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"))

        val forventet: List<OpplysningerOmArbeidssoekerResponse>? = listOf(
            OpplysningerOmArbeidssoekerResponse(
                opplysningerOmArbeidssoekerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
                periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z"),
                    utfoertAv = BrukerResponse(
                        type = BrukerType.SLUTTBRUKER
                    ),
                    kilde = "paw-arbeidssoekerregisteret-inngang",
                    aarsak = "opplysning om arbeidssøker sendt inn"
                ),
                jobbsituasjon = listOf(
                    BeskrivelseMedDetaljerResponse(
                        beskrivelse = JobbSituasjonBeskrivelse.ALDRI_HATT_JOBB,
                        detaljer = emptyMap()
                    )
                ),
                utdanning = UtdanningResponse(
                    nus = "3",
                    bestaatt = JaNeiVetIkke.JA,
                    godkjent = JaNeiVetIkke.JA
                ),
                helse = HelseResponse(
                    helsetilstandHindrerArbeid = JaNeiVetIkke.NEI
                ),
                annet = AnnetResponse(
                    andreForholdHindrerArbeid = JaNeiVetIkke.NEI
                )
            )
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }

    @Test
    fun hentProfilering_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode_og_har_registrert_opplysninger_og_har_profilering() {
        val fnr = Fnr.of("123")

        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRule.port(),
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        val responseBody = """
                    [
                      {
                        "profileringId": "e8f17be2-8b4d-41d5-b86f-acfc51f511c6",
                        "periodeId": "ea0ad984-8b99-4fff-afd6-07737ab19d16",
                        "opplysningerOmArbeidssoekerId": "913161a3-dde9-4448-abf8-2a01a043f8cd",
                        "sendtInnAv": {
                          "tidspunkt": "2024-04-23T13:22:58.668Z",
                          "utfoertAv": {
                            "type": "SYSTEM"
                          },
                          "kilde": "null-null",
                          "aarsak": "opplysninger-mottatt"
                        },
                        "profilertTil": "ANTATT_BEHOV_FOR_VEILEDNING",
                        "jobbetSammenhengendeSeksAvTolvSisteManeder": false,
                        "alder": 42
                      }
                    ]
                """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v1/veileder/profilering")).withRequestBody(
                WireMock.equalToJson(
                    """{"identitetsnummer":"${fnr.get()}","periodeId":"ea0ad984-8b99-4fff-afd6-07737ab19d16"}"""
                )
            ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response: List<ProfileringResponse>? = client.hentProfilering(fnr.get(), UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"))

        val forventet: List<ProfileringResponse>? = listOf(
            ProfileringResponse(
                profileringId = UUID.fromString("e8f17be2-8b4d-41d5-b86f-acfc51f511c6"),
                periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
                opplysningerOmArbeidssoekerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.668Z"),
                    utfoertAv = BrukerResponse(
                        type = BrukerType.SYSTEM
                    ),
                    kilde = "null-null",
                    aarsak = "opplysninger-mottatt"
                ),
                profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
                jobbetSammenhengendeSeksAvTolvSisteManeder = false,
                alder = 42
            )
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }
}