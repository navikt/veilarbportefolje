package no.nav.pto.veilarbportefolje.aap.domene

enum class AapVedtakStatus {
    LØPENDE,
    AVSLUTTET,
    UTREDES;

    companion object {
        fun fraDb(dbString: String?): AapVedtakStatus {
            return when (dbString) {
                "LØPENDE" -> LØPENDE
                "AVSLUTTET" -> AVSLUTTET
                "UTREDES" -> UTREDES
                else -> throw IllegalArgumentException("Ukjent status: $dbString")
            }
        }

    }
}

