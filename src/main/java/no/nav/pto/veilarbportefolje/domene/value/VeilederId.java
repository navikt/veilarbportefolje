package no.nav.pto.veilarbportefolje.domene.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class VeilederId {
    private final String veilederId;

    public VeilederId(@JsonProperty("veilederId") String veilederId) {
        this.veilederId = veilederId;
    }

    @JsonCreator
    public static VeilederId of(String veilederId) {
        return new VeilederId(veilederId);
    }

    @JsonProperty("veilederId")
    public String getValue() {
        return veilederId;
    }

    @Override
    public String toString() {
        return veilederId;
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
