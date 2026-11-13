package no.nav.pto.veilarbportefolje.beslutteroversikt

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.auth.context.UserRole
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.pto.veilarbportefolje.api.beslutteroversikt.BeslutteroversiktController
import no.nav.pto.veilarbportefolje.api.beslutteroversikt.BeslutteroversiktService
import no.nav.pto.veilarbportefolje.api.beslutteroversikt.HentEtiketterRequest
import no.nav.pto.veilarbportefolje.api.beslutteroversikt.KVALITETSSIKRING_14A_VEDTAK_ROLLE
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class BeslutteroversiktControllerTest {

    private val service: BeslutteroversiktService = mock(BeslutteroversiktService::class.java)
    private val authContextHolder = AuthContextHolderThreadLocal.instance()
    private val controller = BeslutteroversiktController(service, authContextHolder)
    private val request = HentEtiketterRequest(listOf(randomFnr().get()))

    @Test
    fun skal_tillate_systemkall_med_rolle() {
        `when`(service.hentEtiketter(request.fnr)).thenReturn(emptyList())

        val resultat = kall(UserRole.SYSTEM, listOf("access_as_application", KVALITETSSIKRING_14A_VEDTAK_ROLLE))

        assertThat(resultat).isEmpty()
    }

    @Test
    fun skal_avvise_systemkall_uten_rolle() {
        sjekkForbidden { kall(UserRole.SYSTEM, listOf("access_as_application")) }
    }

    @Test
    fun skal_avvise_systemkall_uten_access_as_application() {
        sjekkForbidden { kall(UserRole.SYSTEM, listOf(KVALITETSSIKRING_14A_VEDTAK_ROLLE)) }
    }

    @Test
    fun skal_avvise_internbruker() {
        sjekkForbidden { kall(UserRole.INTERN, listOf("access_as_application", KVALITETSSIKRING_14A_VEDTAK_ROLLE)) }
    }

    private fun kall(rolle: UserRole, roles: List<String>) =
        authContextHolder.withContext(
            AuthContext(rolle, PlainJWT(JWTClaimsSet.Builder().claim("roles", roles).build())),
            UnsafeSupplier { controller.hentEtiketter(request) }
        )

    private fun sjekkForbidden(kall: () -> Unit) {
        assertThatThrownBy { kall() }
            .isInstanceOf(ResponseStatusException::class.java)
            .extracting { (it as ResponseStatusException).statusCode }
            .isEqualTo(HttpStatus.FORBIDDEN)
    }
}
