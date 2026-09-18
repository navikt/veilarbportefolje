package no.nav.pto.veilarbportefolje.skjerming

data class SkjermingDTO(
    val skjermetFra: IntArray?,
    val skjermetTil: IntArray?
) {
    // Overrider equals da properties av type *Array bruker referential equality by default
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SkjermingDTO

        if (!skjermetFra.contentEquals(other.skjermetFra)) return false
        if (!skjermetTil.contentEquals(other.skjermetTil)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = skjermetFra?.contentHashCode() ?: 0
        result = 31 * result + (skjermetTil?.contentHashCode() ?: 0)
        return result
    }
}
