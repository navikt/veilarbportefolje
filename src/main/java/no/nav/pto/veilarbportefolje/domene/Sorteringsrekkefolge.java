package no.nav.pto.veilarbportefolje.domene;

public enum Sorteringsrekkefolge {
    IKKE_SATT("ikke_satt"),
    STIGENDE("ascending"),
    SYNKENDE("descending");

    /**
     * Verdien som blir sendt mellom frontend og backend
     */
    public final String sorteringsverdi;

    Sorteringsrekkefolge(String sorteringsverdi) {
        this.sorteringsverdi = sorteringsverdi;
    }

    public static Sorteringsrekkefolge toSorteringsrekkefolge(String sorteringsverdi) {
        for (Sorteringsrekkefolge sorteringsrekkefolge : values()) {
            if (sorteringsrekkefolge.sorteringsverdi.equals(sorteringsverdi)) {
                return sorteringsrekkefolge;
            }
        }
        throw new IllegalArgumentException("Ugyldig verdi for enum: " + sorteringsverdi);
    }

    @Override
    public String toString() {
        return this.name() + " (" + this.sorteringsverdi + ")";
    }
}
