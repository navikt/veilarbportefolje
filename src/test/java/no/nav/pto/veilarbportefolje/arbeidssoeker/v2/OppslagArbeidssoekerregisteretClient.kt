package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.rest.client.RestClient
import no.nav.common.types.identer.Fnr
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

@WireMockTest
class OppslagArbeidssoekerregisteretClientTest {

    @Test
    fun hentArbeidssoekerperioder_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        val fnr = Fnr.of("123")

        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRuntimeInfo.httpPort,
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
    fun hentOpplysningerOmArbeidssoeker_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode_og_har_registrert_opplysninger(
        wireMockRuntimeInfo: WireMockRuntimeInfo
    ) {
        val fnr = Fnr.of("123")

        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRuntimeInfo.httpPort,
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

        val response: List<OpplysningerOmArbeidssoekerResponse>? =
            client.hentOpplysningerOmArbeidssoeker(fnr.get(), UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"))

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
                        beskrivelse = JobbSituasjonBeskrivelseResponse.ALDRI_HATT_JOBB,
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
    fun hentProfilering_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode_og_har_registrert_opplysninger_og_har_profilering(
        wireMockRuntimeInfo: WireMockRuntimeInfo
    ) {
        val fnr = Fnr.of("123")

        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRuntimeInfo.httpPort,
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

        val response: List<ProfileringResponse>? =
            client.hentProfilering(fnr.get(), UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"))

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

    @Test
    fun hentAggregertPeriode_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        val fnr = Fnr.of("01017012345")

        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRuntimeInfo.httpPort,
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        val responseBody = """
            {
              "id": "ea0ad984-8b99-4fff-afd6-07737ab19d16",
              "identitetsnummer": "01017012345",
              "startet": {
                "type": "PERIODE_STARTET_V1",
                "sendtInnAv": {
                  "tidspunkt": "2024-04-23T13:04:40.739Z",
                  "utfoertAv": { "type": "SLUTTBRUKER", "id": "01017012345", "sikkerhetsnivaa": "idporten-loa-high" },
                  "kilde": "paw-arbeidssokerregisteret-api-inngang",
                  "aarsak": "Er over 18 år",
                  "tidspunktFraKilde": null
                },
                "tidspunkt": "2024-04-23T13:04:40.739Z"
              },
              "avsluttet": null,
              "opplysning": {
                "type": "OPPLYSNINGER_V4",
                "id": "913161a3-dde9-4448-abf8-2a01a043f8cd",
                "sendtInnAv": {
                  "tidspunkt": "2024-04-23T13:22:58.089Z",
                  "utfoertAv": { "type": "VEILEDER", "id": "Z123456" },
                  "kilde": "paw-arbeidssokerregisteret-api-inngang",
                  "aarsak": "opplysning om arbeidssøker sendt inn",
                  "tidspunktFraKilde": { "tidspunkt": "2024-04-23T13:22:00Z", "avviksType": "FORSINKELSE" }
                },
                "utdanning": { "nus": "3", "bestaatt": "JA", "godkjent": "UKJENT_VERDI" },
                "helse": { "helsetilstandHindrerArbeid": "NEI" },
                "jobbsituasjon": {
                  "beskrivelser": [
                    { "beskrivelse": "HAR_BLITT_SAGT_OPP", "detaljer": { "prosent": "25" } }
                  ]
                },
                "annet": { "andreForholdHindrerArbeid": "VET_IKKE" },
                "tidspunkt": "2024-04-23T13:22:58.089Z"
              },
              "profilering": {
                "type": "PROFILERING_V1",
                "id": "c4f8b2a1-1111-4c3e-9b1a-2a01a043f8cd",
                "opplysningerOmArbeidssokerId": "913161a3-dde9-4448-abf8-2a01a043f8cd",
                "sendtInnAv": {
                  "tidspunkt": "2024-04-23T13:22:58.089Z",
                  "utfoertAv": { "type": "SYSTEM", "id": "paw-profilering" },
                  "kilde": "null-null",
                  "aarsak": "opplysninger-mottatt"
                },
                "profilertTil": "ANTATT_BEHOV_FOR_VEILEDNING",
                "jobbetSammenhengendeSeksAvTolvSisteMnd": false,
                "alder": 42,
                "tidspunkt": "2024-04-23T13:22:58.089Z"
              },
              "egenvurdering": {
                "type": "EGENVURDERING_V1",
                "id": "d5a9c3b2-2222-4c3e-9b1a-2a01a043f8cd",
                "profileringId": "c4f8b2a1-1111-4c3e-9b1a-2a01a043f8cd",
                "sendtInnAv": {
                  "tidspunkt": "2024-04-24T08:00:00Z",
                  "utfoertAv": { "type": "SLUTTBRUKER", "id": "01017012345" },
                  "kilde": "paw-arbeidssoekerregisteret-api-egenvurdering",
                  "aarsak": "egenvurdering sendt inn"
                },
                "profilertTil": "ANTATT_BEHOV_FOR_VEILEDNING",
                "egenvurdering": "ANTATT_GODE_MULIGHETER",
                "tidspunkt": "2024-04-24T08:00:00Z"
              },
              "bekreftelse": {
                "type": "BEKREFTELSE_V1",
                "id": "e6b0d4c3-3333-4c3e-9b1a-2a01a043f8cd",
                "bekreftelsesloesning": "ARBEIDSSOEKERREGISTERET",
                "status": "GYLDIG",
                "svar": {
                  "sendtInnAv": {
                    "tidspunkt": "2024-05-07T10:00:00Z",
                    "utfoertAv": { "type": "SLUTTBRUKER", "id": "01017012345" },
                    "kilde": "paw-arbeidssoekerregisteret-bekreftelse",
                    "aarsak": "bekreftelse levert"
                  },
                  "gjelderFra": "2024-04-23T00:00:00Z",
                  "gjelderTil": "2024-05-07T00:00:00Z",
                  "harJobbetIDennePerioden": false,
                  "vilFortsetteSomArbeidssoeker": true
                },
                "tidspunkt": "2024-05-07T10:00:00Z"
              }
            }
        """.trimIndent()

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/snapshot"))
                .withHeader("Authorization", WireMock.equalTo("Bearer TOKEN"))
                .withRequestBody(
                    WireMock.equalToJson(
                        "{\"type\":\"IDENTITETSNUMMER\",\"identitetsnummer\":\"${fnr.get()}\"}"
                    )
                ).willReturn(WireMock.aResponse().withStatus(200).withBody(responseBody))
        )

        val response: AggregertPeriodeResponse? = client.hentAggregertPeriode(fnr.get())

        val forventet = AggregertPeriodeResponse(
            id = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
            identitetsnummer = "01017012345",
            startet = PeriodeHendelseResponse(
                sendtInnAv = MetadataV3Response(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:04:40.739Z"),
                    utfoertAv = BrukerV3Response(BrukerType.SLUTTBRUKER, "01017012345", "idporten-loa-high"),
                    kilde = "paw-arbeidssokerregisteret-api-inngang",
                    aarsak = "Er over 18 år",
                    tidspunktFraKilde = null
                ),
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:04:40.739Z")
            ),
            avsluttet = null,
            opplysning = OpplysningerV3Response(
                id = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
                sendtInnAv = MetadataV3Response(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z"),
                    utfoertAv = BrukerV3Response(BrukerType.VEILEDER, "Z123456", null),
                    kilde = "paw-arbeidssokerregisteret-api-inngang",
                    aarsak = "opplysning om arbeidssøker sendt inn",
                    tidspunktFraKilde = TidspunktFraKildeResponse(
                        tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:00Z"),
                        avviksType = AvviksType.FORSINKELSE
                    )
                ),
                utdanning = UtdanningV3Response("3", JaNeiVetIkkeV3.JA, JaNeiVetIkkeV3.UKJENT_VERDI),
                helse = HelseV3Response(JaNeiVetIkkeV3.NEI),
                jobbsituasjon = JobbsituasjonV3Response(
                    listOf(
                        BeskrivelseMedDetaljerResponse(
                            beskrivelse = JobbSituasjonBeskrivelseResponse.HAR_BLITT_SAGT_OPP,
                            detaljer = mapOf("prosent" to "25")
                        )
                    )
                ),
                annet = AnnetV3Response(JaNeiVetIkkeV3.VET_IKKE),
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z")
            ),
            profilering = ProfileringV3Response(
                id = UUID.fromString("c4f8b2a1-1111-4c3e-9b1a-2a01a043f8cd"),
                opplysningerOmArbeidssokerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
                sendtInnAv = MetadataV3Response(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z"),
                    utfoertAv = BrukerV3Response(BrukerType.SYSTEM, "paw-profilering", null),
                    kilde = "null-null",
                    aarsak = "opplysninger-mottatt",
                    tidspunktFraKilde = null
                ),
                profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
                jobbetSammenhengendeSeksAvTolvSisteMnd = false,
                alder = 42,
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z")
            ),
            egenvurdering = EgenvurderingV3Response(
                id = UUID.fromString("d5a9c3b2-2222-4c3e-9b1a-2a01a043f8cd"),
                profileringId = UUID.fromString("c4f8b2a1-1111-4c3e-9b1a-2a01a043f8cd"),
                sendtInnAv = MetadataV3Response(
                    tidspunkt = ZonedDateTime.parse("2024-04-24T08:00:00Z"),
                    utfoertAv = BrukerV3Response(BrukerType.SLUTTBRUKER, "01017012345", null),
                    kilde = "paw-arbeidssoekerregisteret-api-egenvurdering",
                    aarsak = "egenvurdering sendt inn",
                    tidspunktFraKilde = null
                ),
                profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
                egenvurdering = ProfilertTil.ANTATT_GODE_MULIGHETER,
                tidspunkt = ZonedDateTime.parse("2024-04-24T08:00:00Z")
            ),
            bekreftelse = BekreftelseV3Response(
                id = UUID.fromString("e6b0d4c3-3333-4c3e-9b1a-2a01a043f8cd"),
                bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                status = BekreftelseStatus.GYLDIG,
                svar = BekreftelseSvarResponse(
                    sendtInnAv = MetadataV3Response(
                        tidspunkt = ZonedDateTime.parse("2024-05-07T10:00:00Z"),
                        utfoertAv = BrukerV3Response(BrukerType.SLUTTBRUKER, "01017012345", null),
                        kilde = "paw-arbeidssoekerregisteret-bekreftelse",
                        aarsak = "bekreftelse levert",
                        tidspunktFraKilde = null
                    ),
                    gjelderFra = ZonedDateTime.parse("2024-04-23T00:00:00Z"),
                    gjelderTil = ZonedDateTime.parse("2024-05-07T00:00:00Z"),
                    harJobbetIDennePerioden = false,
                    vilFortsetteSomArbeidssoeker = true
                ),
                tidspunkt = ZonedDateTime.parse("2024-05-07T10:00:00Z")
            )
        )

        Assertions.assertThat(response).isEqualTo(forventet)
    }

    @Test
    fun hentAggregertPeriode_gir_null_naar_bruker_ikke_har_arbeidssoekerperiode(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        val client = OppslagArbeidssoekerregisteretClient(
            "http://localhost:" + wireMockRuntimeInfo.httpPort,
            { "TOKEN" },
            RestClient.baseClient(),
            "veilarbportefolje"
        )

        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/snapshot"))
                .willReturn(WireMock.aResponse().withStatus(404))
        )

        Assertions.assertThat(client.hentAggregertPeriode("01017012345")).isNull()
    }
}
