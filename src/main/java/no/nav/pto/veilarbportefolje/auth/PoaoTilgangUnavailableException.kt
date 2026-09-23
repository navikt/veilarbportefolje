package no.nav.pto.veilarbportefolje.auth

/**
 * Denne exception-typen brukes av GlobalExceptionHandler.)
 *
 * Exceptionen betyr "vi vet ikke om veilederen har tilgang", og
 * skal ALDRI tolkes som at tilgang er gitt.
 *
 * Inneholder bevisst ikke fnr eller andre personopplysninger - kun policy-typen som
 * var degradert, slik at meldingen trygt kan logges og eventuelt inngå i et 503-svar.
 */
class PoaoTilgangUnavailableException(policyNavn: String) :
    RuntimeException("poao-tilgang er midlertidig utilgjengelig for policy-type '$policyNavn'")
