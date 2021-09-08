package no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak;

import lombok.Value;
import lombok.experimental.Wither;
import no.nav.common.types.identer.Fnr;

import java.sql.Timestamp;

@Value(staticConstructor = "of")
@Wither
public class Brukertiltak {
    private Fnr fnr;
    private String tiltak;
    private Timestamp tildato;

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