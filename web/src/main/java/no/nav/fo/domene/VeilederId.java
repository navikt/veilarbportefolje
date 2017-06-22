package no.nav.fo.domene;

import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class VeilederId {
    String veilederId;

    @Override
    public String toString() {
        return veilederId;
    }
}
