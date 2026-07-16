package no.nav.pto.veilarbportefolje.lagredefilter.minefilter

import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg


enum class LagredeFilterFeilmeldinger(val message: String) {
    NAVN_FOR_LANGT("Lengden på navnet kan ikke være mer enn 255 karakterer"),
    NAVN_TOMT("Navn kan ikke være tomt"),
    FILTERVALG_TOMT("Filtervalg kan ikke være tomt"),
    NAVN_EKSISTERER("Navn eksisterer i et annet lagret filter"),
    FILTERVALG_EKSISTERER("Filterkombinasjon eksisterer i et annet lagret filter"),
}

private const val MAKS_LENGDE_FILTER_NAVN = 255

fun validerFilterNavn(filterNavn: String): LagredeFilterFeilmeldinger? = when {
    filterNavn.isBlank() -> LagredeFilterFeilmeldinger.NAVN_TOMT
    filterNavn.length > MAKS_LENGDE_FILTER_NAVN -> LagredeFilterFeilmeldinger.NAVN_FOR_LANGT
    else -> null
}

fun validerFiltervalg(filtervalg: Filtervalg): LagredeFilterFeilmeldinger? =
    if (!filtervalg.harAktiveFilter()) LagredeFilterFeilmeldinger.FILTERVALG_TOMT else null

fun validerUnikhet(navnEksisterer: Boolean, valgEksisterer: Boolean): LagredeFilterFeilmeldinger? = when {
    navnEksisterer -> LagredeFilterFeilmeldinger.NAVN_EKSISTERER
    valgEksisterer -> LagredeFilterFeilmeldinger.FILTERVALG_EKSISTERER
    else -> null
}
