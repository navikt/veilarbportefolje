package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.common.rest.client.RestClient
import no.nav.pto.veilarbportefolje.util.TestUtil.readTestResourceFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

@WireMockTest
class ArbeidssoekerregisterClientTest {

    private val identitetsnummer = "23916795505"

    private fun lagClient(wireMockRuntimeInfo: WireMockRuntimeInfo) = ArbeidssoekerregisterClient(
        "http://localhost:" + wireMockRuntimeInfo.httpPort,
        { "TOKEN" },
        RestClient.baseClient(),
        "veilarbportefolje"
    )

    @Test
    fun hentAggregertArbeidssokerPeriode_gir_forventet_respons_naar_bruker_har_aktiv_arbeidssoekerperiode(
        wireMockRuntimeInfo: WireMockRuntimeInfo
    ) {
        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/snapshot"))
                .withHeader("Authorization", WireMock.equalTo("Bearer TOKEN"))
                .withHeader("Nav-Consumer-Id", WireMock.equalTo("veilarbportefolje"))
                .withRequestBody(
                    WireMock.equalToJson(
                        """{"type":"IDENTITETSNUMMER","identitetsnummer":"$identitetsnummer"}"""
                    )
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody(readTestResourceFile("aggregertArbeidssoekerPeriode.json"))
                )
        )

        val response = lagClient(wireMockRuntimeInfo).hentAggregertArbeidssokerPeriode(identitetsnummer)

        val forventet = AggregertPeriodeResponse(
            id = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"),
            identitetsnummer = identitetsnummer,
            startet = PeriodeHendelseResponse(
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:04:40.739Z"),
                    utfoertAv = BrukerResponse(
                        type = BrukerType.SLUTTBRUKER,
                        id = identitetsnummer,
                        sikkerhetsnivaa = null
                    ),
                    kilde = "paw-arbeidssokerregisteret-api-inngang",
                    aarsak = "Er over 18 år, er bosatt i Norge i hendhold Folkeregisterloven",
                    tidspunktFraKilde = null
                ),
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:04:40.739Z")
            ),
            avsluttet = null,
            opplysning = OpplysningerResponse(
                id = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z"),
                    utfoertAv = BrukerResponse(
                        type = BrukerType.SLUTTBRUKER,
                        id = identitetsnummer,
                        sikkerhetsnivaa = null
                    ),
                    kilde = "paw-arbeidssoekerregisteret-inngang",
                    aarsak = "opplysning om arbeidssøker sendt inn",
                    tidspunktFraKilde = null
                ),
                utdanning = UtdanningResponse(
                    nus = "3",
                    bestaatt = JaNeiVetIkke.JA,
                    godkjent = JaNeiVetIkke.JA
                ),
                helse = HelseResponse(helsetilstandHindrerArbeid = JaNeiVetIkke.NEI),
                jobbsituasjon = JobbsituasjonResponse(
                    beskrivelser = listOf(
                        BeskrivelseMedDetaljerResponse(
                            beskrivelse = JobbSituasjonBeskrivelseResponse.ALDRI_HATT_JOBB,
                            detaljer = mapOf("prosent" to "25")
                        ),
                        BeskrivelseMedDetaljerResponse(
                            beskrivelse = JobbSituasjonBeskrivelseResponse.ER_PERMITTERT,
                            detaljer = mapOf("prosent" to "75")
                        )
                    )
                ),
                annet = AnnetResponse(andreForholdHindrerArbeid = JaNeiVetIkke.NEI),
                tidspunkt = ZonedDateTime.parse("2024-04-23T13:22:58.089Z")
            ),
            profilering = ProfileringResponse(
                id = UUID.fromString("7c9a2feb-0b31-4101-825a-0c4a3d465e01"),
                opplysningerOmArbeidssokerId = UUID.fromString("913161a3-dde9-4448-abf8-2a01a043f8cd"),
                sendtInnAv = MetadataResponse(
                    tidspunkt = ZonedDateTime.parse("2024-04-25T08:19:53.612Z"),
                    utfoertAv = BrukerResponse(
                        type = BrukerType.SYSTEM,
                        id = "paw-profilering",
                        sikkerhetsnivaa = null
                    ),
                    kilde = "null-null",
                    aarsak = "opplysninger-mottatt",
                    tidspunktFraKilde = null
                ),
                profilertTil = ProfilertTil.OPPGITT_HINDRINGER,
                jobbetSammenhengendeSeksAvTolvSisteMnd = false,
                alder = 33,
                tidspunkt = ZonedDateTime.parse("2024-04-25T08:19:53.612Z")
            ),
            egenvurdering = null,
            bekreftelse = null
        )

        assertThat(response).isEqualTo(forventet)
    }

    @Test
    fun hentAggregertArbeidssokerPeriode_gir_null_naar_bruker_ikke_finnes(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/snapshot"))
                .willReturn(WireMock.aResponse().withStatus(404))
        )

        val response = lagClient(wireMockRuntimeInfo).hentAggregertArbeidssokerPeriode(identitetsnummer)

        assertThat(response).isNull()
    }

    @Test
    fun hentAggregertArbeidssokerPeriode_kaster_exception_ved_feilrespons(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        WireMock.givenThat(
            WireMock.post(WireMock.urlEqualTo("/api/v3/snapshot"))
                .willReturn(WireMock.aResponse().withStatus(500))
        )

        val client = lagClient(wireMockRuntimeInfo)

        assertThatThrownBy { client.hentAggregertArbeidssokerPeriode(identitetsnummer) }
            .isInstanceOf(RuntimeException::class.java)
    }
}
