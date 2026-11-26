package no.nav.pto.veilarbportefolje.domene.filtervalg

/**
 * De ulike aktivitetstypene som er mulig å filtrere på.
 */
enum class AktivitetFilterverdier {
    SOKEAVTALE,
    STILLING,
    BEHANDLING,
    TILTAK,
    EGEN,
    IJOBB,
    MOTE,
    GRUPPEAKTIVITET,
    UTDANNINGAKTIVITET,
}

/**
 * Ved bruk av "avansert filter" i frontend får man mulighet til å filtrere med [JA], [NEI] eller [NA] (default) for
 * hvert enkelt aktivitetstype i [AktivitetFilterverdier].
 */
enum class AktivitetAvansertFilterverdier {
    JA, NEI, NA
}