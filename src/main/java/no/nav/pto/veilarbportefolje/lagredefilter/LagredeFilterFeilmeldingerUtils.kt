package no.nav.pto.veilarbportefolje.lagredefilter

private const val MAKS_LENGDE_FILTER_NAVN = 255

fun harGyldigFilterNavn(filterNavn: String): Boolean =
    filterNavn.isNotBlank() && filterNavn.length <= MAKS_LENGDE_FILTER_NAVN

fun harUniktNavnOgFiltervalg(navnEksisterer: Boolean, valgEksisterer: Boolean): Boolean =
    !navnEksisterer && !valgEksisterer
