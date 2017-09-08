package no.nav.fo.domene;

import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value(staticConstructor = "of")
@Getter(value = PRIVATE)
public class VeilederId {
    public final String veilederId;

    @Override
    public String toString() {
        return veilederId;
    }
}
