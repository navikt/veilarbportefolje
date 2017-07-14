package no.nav.fo.domene;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class VeilederId {
    public final String veilederId;

    @Override
    @JsonValue
    public String toString() {
        return veilederId;
    }
}
