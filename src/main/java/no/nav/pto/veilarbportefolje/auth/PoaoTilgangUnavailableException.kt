package no.nav.pto.veilarbportefolje.auth

/**
 * Kastes når et poao-tilgang-kall kortsluttes fordi policy-typen er markert degradert
 * etter nylige feil/timeouts - altså IKKE et faktisk nettverkskall som feilet denne
 * gangen. (Kortslutningsmekanismen selv - en fail-fast/cooldown-guard - er utsatt til
 * en senere PR, se plan.md punkt 2. Denne exception-typen brukes allerede av
 * GlobalExceptionHandler, punkt 3, og beholdes derfor.)
 *
 * Fail-closed: denne exceptionen betyr "vi vet ikke om veilederen har tilgang", og
 * skal ALDRI tolkes som at tilgang er gitt. Se plan.md punkt 2 og 3.
 *
 * Inneholder bevisst ikke fnr eller andre personopplysninger - kun policy-typen som
 * var degradert, slik at meldingen trygt kan logges og eventuelt inngå i et 503-svar.
 */
class PoaoTilgangUnavailableException(policyNavn: String) :
    RuntimeException("poao-tilgang er midlertidig utilgjengelig for policy-type '$policyNavn' (fail-fast-cooldown aktiv)")
