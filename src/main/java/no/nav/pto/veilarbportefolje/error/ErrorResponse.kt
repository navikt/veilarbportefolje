package no.nav.pto.veilarbportefolje.error

/**
 * Strukturert feil-body som returneres av [GlobalExceptionHandler].
 *
 * Inneholder BEVISST ikke fnr, aktørId eller andre personopplysninger - kun
 * informasjon som er trygg å sende til frontend og logge. Se plan.md punkt 3.
 */
data class ErrorResponse(val melding: String, val korrelasjonsId: String)
