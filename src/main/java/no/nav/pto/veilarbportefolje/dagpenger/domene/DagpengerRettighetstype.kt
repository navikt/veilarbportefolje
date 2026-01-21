package no.nav.pto.veilarbportefolje.dagpenger.domene

enum class DagpengerRettighetstype {
    DAGPENGER_ARBEIDSSOKER_ORDINAER,
    DAGPENGER_PERMITTERING_ORDINAER,
    DAGPENGER_PERMITTERING_FISKEINDUSTRI;

    companion object {
        fun tilFrontendtekst(rettighet: DagpengerRettighetstype): String {

            return when (rettighet) {
                DAGPENGER_ARBEIDSSOKER_ORDINAER -> "Ordinære dagpenger"
                DAGPENGER_PERMITTERING_ORDINAER -> "Dagpenger under permittering"
                DAGPENGER_PERMITTERING_FISKEINDUSTRI -> "Dagpenger v/perm fiskeindustri"
            }
        }

        fun fraDb(dbString: String?): DagpengerRettighetstype {
            return when (dbString) {
                "DAGPENGER_ARBEIDSSOKER_ORDINAER" -> DAGPENGER_ARBEIDSSOKER_ORDINAER
                "DAGPENGER_PERMITTERING_ORDINAER" -> DAGPENGER_PERMITTERING_ORDINAER
                "DAGPENGER_PERMITTERING_FISKEINDUSTRI" -> DAGPENGER_PERMITTERING_FISKEINDUSTRI
                else -> throw IllegalArgumentException("Ukjent rettighetstype: $dbString")
            }
        }
    }
}

