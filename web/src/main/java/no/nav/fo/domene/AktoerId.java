package no.nav.fo.domene;

import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class AktoerId {
    public final String aktoerId;

    @Override
    public String toString() {
        return aktoerId;
    }

    public static AktoerId of(String aktoerid) {
        return new AktoerId(aktoerid);
    }
}
