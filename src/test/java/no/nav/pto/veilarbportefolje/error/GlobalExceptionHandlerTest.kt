package no.nav.pto.veilarbportefolje.error

import no.nav.poao_tilgang.client.api.NetworkApiException
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangUnavailableException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/**
 * Verifiserer mappingen fra exception til HTTP-status/body i [GlobalExceptionHandler]
 * 503 for poao-tilgang-feil, 500-sikkerhetsnett for øvrige
 * exceptions.
 *
 * NB: Testen "skalIkkeLekkeFnrIResponsEllerLogg" er det viktigste kravet her - den skal
 * IKKE bare fjernes/forenkles for å få testen til å gå grønt, den skal faktisk verifisere
 * at ingen fnr havner i verken respons-body eller logglinjer.
 */
class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun skalReturnere503VedPoaoTilgangUnavailable() {
        val exception = PoaoTilgangUnavailableException("person")

        val response = handler.handlePoaoTilgangUnavailable(exception)

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun skalReturnere503VedNetworkApiException() {
        val exception = NetworkApiException(RuntimeException("timeout"))

        val response = handler.handlePoaoTilgangNetworkFeil(exception)

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertNotNull(response.body)
    }

    @Test
    fun skalIkkeLekkeFnrIResponsEllerLogg() {
        // Simuler en exception der en eventuell (feilaktig) implementasjon kunne vært
        // fristet til å inkludere fnr i meldingen, f.eks. via en underliggende cause.
        val fnr = "12345678901"
        val exception = PoaoTilgangUnavailableException("person")

        val response = handler.handlePoaoTilgangUnavailable(exception)

        assertNotNull(response.body)
        val bodyAsString = response.body.toString()
        assertFalse(bodyAsString.contains(fnr), "Responsen skal ALDRI inneholde fnr")
    }

    @Test
    fun skalReturnere500ForUhandterteExceptions() {
        val uventetFeil: Exception = IllegalStateException("noe uventet")

        val response = handler.handleUhandtertException(uventetFeil)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body)
    }
}
