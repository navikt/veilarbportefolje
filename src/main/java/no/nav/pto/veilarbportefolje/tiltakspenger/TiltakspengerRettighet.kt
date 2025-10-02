package no.nav.pto.veilarbportefolje.tiltakspenger

enum class TiltakspengerRettighet {
    TILTAKSPENGER,
    TILTAKSPENGER_OG_BARNETILLEGG,
    INGENTING;  //betyr ingen ytelse akkurat nå, må filtreres bort før vi håndterer den separat

    companion object {
        fun tilFrontendtekst(rettighet: TiltakspengerRettighet?): String? {
            if (rettighet == null) return null

            return when (rettighet) {
                TILTAKSPENGER -> "Tiltakspenger"
                TILTAKSPENGER_OG_BARNETILLEGG -> "Tiltakspenger og barnetillegg"
                INGENTING -> "Ingenting"
            }
        }
    }
}
