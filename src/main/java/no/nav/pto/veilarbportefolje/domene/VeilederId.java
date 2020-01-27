package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class VeilederId {
    public final String veilederId;

    @Override
    public String toString() {
        return veilederId;
    }

    public static VeilederId of(String veilederId) {
        return new VeilederId(veilederId);
    }

    @JsonCreator
    public VeilederId(@JsonProperty("veilederId") String veilederId) {
        this.veilederId = veilederId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VeilederId that = (VeilederId) o;

        return veilederId.equals(that.veilederId);
    }

    @Override
    public int hashCode() {
        return veilederId.hashCode();
    }
}
