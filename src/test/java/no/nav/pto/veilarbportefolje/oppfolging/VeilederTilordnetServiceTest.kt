package no.nav.pto.veilarbportefolje.oppfolging

import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.domene.VeilederId.Companion.of
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingClient
import no.nav.pto.veilarbportefolje.oppfolging.dto.VeilederTilordnetDTO
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime

internal class VeilederTilordnetServiceTest @Autowired constructor(
    private val aktorClient: AktorClient,
    private val oppfolgingClient: OppfolgingClient,
    private val veilederTilordnetService: VeilederTilordnetService
) : EndToEndTest() {
    @Test
    fun skal_oppdatere_tilordnet_veileder() {
        val aktoerId = randomAktorId()
        val nyVeileder = randomVeilederId()
        val tilordnet = ZonedDateTime.now()
        val forventetTildeltTidspunkt = tilordnet.toLocalDateTime()
        Mockito.`when`(aktorClient.hentFnr(aktoerId)).thenReturn(randomFnr())
        Mockito.`when`(oppfolgingClient.hentUnderOppfolging(aktoerId)).thenReturn(true)

        testDataClient.lagreBrukerUnderOppfolging(
            aktoerId,
            randomNavKontor(),
            randomVeilederId(),
            ZonedDateTime.now(),
            null
        )

        veilederTilordnetService.behandleKafkaMeldingLogikk(VeilederTilordnetDTO(aktoerId, nyVeileder, tilordnet))

        val bruker = opensearchTestClient.hentBrukerFraOpensearch(aktoerId)
        val tilordnetVeileder = of(bruker.veileder_id)

        AssertionsForClassTypes.assertThat(tilordnetVeileder).isEqualTo(nyVeileder)
        AssertionsForClassTypes.assertThat(bruker.ny_for_veileder).isTrue()
        AssertionsForClassTypes.assertThat(bruker.tildelt_tidspunkt).isEqualTo(forventetTildeltTidspunkt)
    }

    @Test
    fun skal_oppdatere_tilordnet_veileder_med_null() {
        val aktoerId = randomAktorId()
        val nyVeileder = of(null)
        Mockito.`when`(aktorClient.hentFnr(aktoerId)).thenReturn(randomFnr())
        Mockito.`when`(oppfolgingClient.hentUnderOppfolging(aktoerId)).thenReturn(true)

        testDataClient.lagreBrukerUnderOppfolging(
            aktoerId,
            randomNavKontor(),
            randomVeilederId(),
            ZonedDateTime.now(),
            null
        )

        veilederTilordnetService.behandleKafkaMeldingLogikk(VeilederTilordnetDTO(aktoerId, nyVeileder, null))

        val bruker = opensearchTestClient.hentBrukerFraOpensearch(aktoerId)
        val tilordnetVeileder = of(bruker.veileder_id)

        AssertionsForClassTypes.assertThat(tilordnetVeileder.value).isNull()
        AssertionsForClassTypes.assertThat(bruker.ny_for_veileder).isTrue()
        AssertionsForClassTypes.assertThat(bruker.tildelt_tidspunkt).isNull()
    }
}
