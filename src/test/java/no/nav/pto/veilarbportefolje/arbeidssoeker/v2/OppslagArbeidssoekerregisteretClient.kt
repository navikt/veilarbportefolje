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
    fun hentAggregertArbeidssokerPeriode_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode(wireMockRuntimeInfo: WireMockRuntimeInfo) {
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

        val response: AggregertPeriodeResponse? = client.hentAggregertArbeidssokerPeriode(fnr.get())

        val forventet = AggregertPeriodeResponse(
            id = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
            identitetsnummer = "01017012345",
            startet = PeriodeHendelseResponse(
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:04:40.739Z"),
                    utfoertAv = BrukerResponse(BrukerType.SLUTTBRUKER, "01017012345", "idporten-loa-high"),
                    kilde = "paw-arbeidssokerregisteret-api-inngang",
                    aarsak = "Er over 18 år",
                    tidspunktFraKilde = null
                ),
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:04:40.739Z")
            ),
            avsluttet = null,
            opplysning = OpplysningerResponse(
                id = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z"),
                    utfoertAv = BrukerResponse(BrukerType.VEILEDER, "Z123456", null),
                    kilde = "paw-arbeidssokerregisteret-api-inngang",
                    aarsak = "opplysning om arbeidssøker sendt inn",
                    tidspunktFraKilde = TidspunktFraKildeResponse(
                        tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:00Z"),
                        avviksType = AvviksType.FORSINKELSE
                    )
                ),
                utdanning = UtdanningResponse("3", JaNeiVetIkke.JA, JaNeiVetIkke.UKJENT_VERDI),
                helse = HelseResponse(JaNeiVetIkke.NEI),
                jobbsituasjon = JobbsituasjonResponse(
                    listOf(
                        BeskrivelseMedDetaljerResponse(
                            beskrivelse = JobbSituasjonBeskrivelseResponse.HAR_BLITT_SAGT_OPP,
                            detaljer = mapOf("prosent" to "25")
                        )
                    )
                ),
                annet = AnnetResponse(JaNeiVetIkke.VET_IKKE),
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z")
            ),
            profilering = ProfileringResponse(
                id = UUID.fromString("c4f8b2a1-1111-4c3e-9b1a-2a01a043f8cd"),
                opplysningerOmArbeidssokerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z"),
                    utfoertAv = BrukerResponse(BrukerType.SYSTEM, "paw-profilering", null),
                    kilde = "null-null",
                    aarsak = "opplysninger-mottatt",
                    tidspunktFraKilde = null
                ),
                profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
                jobbetSammenhengendeSeksAvTolvSisteMnd = false,
                alder = 42,
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z")
            ),
            egenvurdering = EgenvurderingResponse(
                id = UUID.fromString("d5a9c3b2-2222-4c3e-9b1a-2a01a043f8cd"),
                profileringId = UUID.fromString("c4f8b2a1-1111-4c3e-9b1a-2a01a043f8cd"),
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-24T08:00:00Z"),
                    utfoertAv = BrukerResponse(BrukerType.SLUTTBRUKER, "01017012345", null),
                    kilde = "paw-arbeidssoekerregisteret-api-egenvurdering",
                    aarsak = "egenvurdering sendt inn",
                    tidspunktFraKilde = null
                ),
                profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
                egenvurdering = ProfilertTil.ANTATT_GODE_MULIGHETER,
                tidspunkt = ZonedDateTime.parse("2024-04-24T08:00:00Z")
            ),
            bekreftelse = BekreftelseResponse(
                id = UUID.fromString("e6b0d4c3-3333-4c3e-9b1a-2a01a043f8cd"),
                bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                status = BekreftelseStatus.GYLDIG,
                svar = BekreftelseSvarResponse(
                    sendtInnAv = MetadataResponse(
                        tidspunkt = ZonedDateTime.parse("2024-05-07T10:00:00Z"),
                        utfoertAv = BrukerResponse(BrukerType.SLUTTBRUKER, "01017012345", null),
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
    fun hentAggregertArbeidssokerPeriode_gir_null_naar_bruker_ikke_har_arbeidssoekerperiode(wireMockRuntimeInfo: WireMockRuntimeInfo) {
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

        Assertions.assertThat(client.hentAggregertArbeidssokerPeriode("01017012345")).isNull()
    }
}
