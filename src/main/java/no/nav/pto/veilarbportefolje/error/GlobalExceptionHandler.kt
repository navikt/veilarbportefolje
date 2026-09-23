package no.nav.pto.veilarbportefolje.error

import no.nav.common.log.MDCConstants.MDC_CALL_ID
import no.nav.poao_tilgang.client.api.NetworkApiException
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangUnavailableException
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Fanger opp poao-tilgang-feil og alle andre ikke-håndterte exceptions som ellers ville
 * gitt "uncaught exception" i logg og en generisk 500 (se plan.md, punkt 3, for full
 * bakgrunn/logganalyse).
 *
 * To harde krav all mapping her overholder:
 * - **Fail-closed:** [PoaoTilgangUnavailableException] og [NetworkApiException] gir 503 -
 *   ALDRI noe som kan tolkes som at tilgang er gitt.
 * - **Ingen PII i logg eller respons:** catch-all-handleren kan fange HVA SOM HELST fra
 *   hele appen (se f.eks. FargekategoriService, som logger fnr i exception-meldinger) -
 *   derfor logges den via [secureLog], og ingen av handlerne bruker `e.message` i
 *   [ErrorResponse]. Se GlobalExceptionHandlerTest for testene som verifiserer dette.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(PoaoTilgangUnavailableException::class)
    fun handlePoaoTilgangUnavailable(e: PoaoTilgangUnavailableException): ResponseEntity<ErrorResponse> {
        val korrelasjonsId = MDC.get(MDC_CALL_ID)
        log.warn("PoaoTilgangUnavailableException", e)
        return ResponseEntity.status(503).body(ErrorResponse(
            melding = "Tilgangskontroll er ikke tilgjengelig",
            korrelasjonsId = korrelasjonsId ?: ""
        ))
    }

    @ExceptionHandler(NetworkApiException::class)
    fun handlePoaoTilgangNetworkFeil(e: NetworkApiException): ResponseEntity<ErrorResponse> {
        // Samme fail-closed 503-mapping som over. Dekker tilfeller der en fremtidig
        // fail-fast/cooldown-guard (utsatt til egen PR, se plan.md punkt 2) ikke kortslutter
        // kallet, men selve nettverkskallet mot poao-tilgang likevel feiler/timer ut.
        val korrelasjonsId = MDC.get(MDC_CALL_ID)
        log.warn("NetworkApiException", e)
        return ResponseEntity.status(503).body(ErrorResponse(
            melding = "Tilgangskontroll er ikke tilgjengelig",
            korrelasjonsId = korrelasjonsId ?: "",
        ))
    }

    @ExceptionHandler(Exception::class)
    fun handleUhandtertException(e: Exception): ResponseEntity<ErrorResponse> {
        val korrelasjonsId = MDC.get(MDC_CALL_ID)
        // Denne handleren kan fange HVA SOM HELST i hele appen - vi kan ikke anta at
        // e.message/stacktrace er PII-fritt (se f.eks. FargekategoriService som logger
        // fnr i exception-meldinger). Bruk derfor secureLog (samme mønster som resten
        // av appen), ikke den vanlige log, for å unngå at fnr havner i åpen logg.
        secureLog.error("Uhandtert exception", e)
        return ResponseEntity.status(500).body(ErrorResponse(
            melding = "Ukjent feil",
            korrelasjonsId = korrelasjonsId ?: ""
        ))
    }
}
