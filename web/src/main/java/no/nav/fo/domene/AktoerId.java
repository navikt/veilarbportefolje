package no.nav.fo.domene;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class AktoerId {
    public final String aktoerId;

    @Override
    @JsonValue
    public String toString() {
        return aktoerId;
    }
}
