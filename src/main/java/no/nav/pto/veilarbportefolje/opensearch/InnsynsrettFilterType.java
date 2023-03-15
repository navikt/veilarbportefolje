package no.nav.pto.veilarbportefolje.opensearch;
public enum InnsynsrettFilterType {
    /**
     * Alle brukere som veileder har innsynsrett på
     */
    ALLE_BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ,

    /**
     * Alle brukere inkludert de som veileder eventuelt ikke har innsynsrett på
     */
    ALLE_BRUKERE,

    /**
     * Kun brukere som veileder eventuelt ikke har innsynsrett på
     */
    BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ
}