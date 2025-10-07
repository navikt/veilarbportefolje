package no.nav.pto.veilarbportefolje.tiltakspenger.domene

enum class TiltakspengerRettighet {
    TILTAKSPENGER,
    TILTAKSPENGER_OG_BARNETILLEGG,
    INGENTING;

    companion object {
        fun tilFrontendtekst(rettighet: TiltakspengerRettighet?): String? {
            if (rettighet == null) return null

            return when (rettighet) {
                TILTAKSPENGER -> "Tiltakspenger"
                TILTAKSPENGER_OG_BARNETILLEGG -> "Tiltakspenger og barnetillegg"
                INGENTING -> "Ingenting"
            }
        }

        fun fraDb(dbString: String?): TiltakspengerRettighet {
            return when (dbString) {
                "TILTAKSPENGER" -> TILTAKSPENGER
                "TILTAKSPENGER_OG_BARNETILLEGG" -> TILTAKSPENGER_OG_BARNETILLEGG
                "INGENTING" -> INGENTING
                else -> throw IllegalArgumentException("Ukjent rettighetstype: $dbString")
            }
        }
    }
}
