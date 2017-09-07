package no.nav.fo.domene;

import lombok.Value;
import lombok.experimental.Wither;

@Value(staticConstructor = "of")
@Wither
public class Brukertiltak {
    private Fnr fnr;
    private String tiltak;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Brukertiltak that = (Brukertiltak) o;

        if (!fnr.equals(that.fnr)) return false;
        return tiltak.equals(that.tiltak);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + fnr.hashCode();
        result = 31 * result + tiltak.hashCode();
        return result;
    }
}
