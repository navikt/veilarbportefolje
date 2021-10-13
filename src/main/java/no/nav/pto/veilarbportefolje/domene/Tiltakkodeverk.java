package no.nav.pto.veilarbportefolje.domene;

import lombok.Value;

@Value(staticConstructor = "of")
public class Tiltakkodeverk {
    private String kode;
    private String verdi;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Tiltakkodeverk that = (Tiltakkodeverk) o;

        return kode.equalsIgnoreCase(that.kode);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + kode.hashCode();
        return result;
    }
}
