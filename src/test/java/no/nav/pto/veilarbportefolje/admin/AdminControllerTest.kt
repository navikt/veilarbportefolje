package no.nav.pto.veilarbportefolje.admin

import com.nimbusds.jwt.JWTClaimsSet
import io.getunleash.DefaultUnleash
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.types.identer.AktorId
import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.aap.AapService
import no.nav.pto.veilarbportefolje.admin.dto.AdminDataForBrukerRequest
import no.nav.pto.veilarbportefolje.admin.dto.AdminDataType
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerService
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.ArbeidssoekerregisterService
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerService
import no.nav.pto.veilarbportefolje.ensligforsorger.EnsligeForsorgereService
import no.nav.pto.veilarbportefolje.opensearch.HovedIndekserer
import no.nav.pto.veilarbportefolje.opensearch.OpensearchAdminService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingClient
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.PdlService
import no.nav.pto.veilarbportefolje.sykmeldt.SykmeldtService
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerService
import no.nav.pto.veilarbportefolje.uforetrygd.UforetrygdService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class AdminControllerTest {
    private val authContextHolder = mock(AuthContextHolder::class.java)
    private val dagpengerService = mock(DagpengerService::class.java)
    private val aapService = mock(AapService::class.java)
    private val controller = AdminController(
        aktorClient = mock(AktorClient::class.java),
        hovedIndekserer = mock(HovedIndekserer::class.java),
        opensearchIndexer = mock(OpensearchIndexer::class.java),
        authContextHolder = authContextHolder,
        oppfolgingRepositoryV2 = mock(OppfolgingRepositoryV2::class.java),
        opensearchAdminService = mock(OpensearchAdminService::class.java),
        pdlService = mock(PdlService::class.java),
        pdlIdentRepository = mock(PdlIdentRepository::class.java),
        ensligForsorgerService = mock(EnsligeForsorgereService::class.java),
        aapService = aapService,
        oppfolgingClient = mock(OppfolgingClient::class.java),
        uforetrygdService = mock(UforetrygdService::class.java),
        sykmeldtService = mock(SykmeldtService::class.java),
        dagpengerService = dagpengerService,
        tiltakspengerService = mock(TiltakspengerService::class.java),
        arbeidssoekerService = mock(ArbeidssoekerService::class.java),
        arbeidssoekerregisterService = mock(ArbeidssoekerregisterService::class.java),
        defaultUnleash = mock(DefaultUnleash::class.java)
    )
    private val aktorId = AktorId.of("1234567890123")
    private val request = AdminDataForBrukerRequest(
        listOf(aktorId),
        listOf(AdminDataType.DAGPENGER_DATA, AdminDataType.AAP_DATA)
    )

    @Test
    fun `returnerer jobbreferanse uten å vente på datahentingen`() {
        giAdminTilgang()
        val startet = CountDownLatch(1)
        val fortsett = CountDownLatch(1)
        val ferdig = CountDownLatch(1)
        doAnswer {
            startet.countDown()
            check(fortsett.await(10, SECONDS))
            null
        }.`when`(dagpengerService).hentOgLagreDagpengerForBrukerVedOppfolgingStart(aktorId)
        doAnswer {
            ferdig.countDown()
            null
        }.`when`(aapService).hentOgLagreAapForBrukerVedOppfolgingStart(aktorId)

        val executor = Executors.newSingleThreadExecutor()
        try {
            val svar = executor.submit<ResponseEntity<String>> {
                controller.hentDataForBrukerForAngitteValg(request)
            }.get(5, SECONDS)

            assertThat(svar.statusCode).isEqualTo(HttpStatus.ACCEPTED)
            assertThat(svar.body).isNotBlank()
            assertThat(startet.await(5, SECONDS)).isTrue()
            verifyNoInteractions(aapService)
        } finally {
            fortsett.countDown()
            executor.shutdownNow()
            assertThat(ferdig.await(5, SECONDS)).isTrue()
        }
        verify(aapService).hentOgLagreAapForBrukerVedOppfolgingStart(aktorId)
    }

    @Test
    fun `avviser tomme valg før jobben starter`() {
        giAdminTilgang()

        val svar = controller.hentDataForBrukerForAngitteValg(request.copy(valg = emptyList()))

        assertThat(svar.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        verifyNoInteractions(dagpengerService, aapService)
    }

    @Test
    fun `avviser kall uten admintilgang før jobben starter`() {
        assertThatThrownBy { controller.hentDataForBrukerForAngitteValg(request) }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.FORBIDDEN)
        verifyNoInteractions(dagpengerService, aapService)
    }

    private fun giAdminTilgang() {
        val cluster = if (EnvironmentUtils.isProduction().orElse(false)) "prod-gcp" else "dev-gcp"
        `when`(authContextHolder.erInternBruker()).thenReturn(true)
        `when`(authContextHolder.getIdTokenClaims()).thenReturn(
            Optional.of(JWTClaimsSet.Builder().claim("azp_name", "$cluster:poao:poao-admin").build())
        )
    }
}
