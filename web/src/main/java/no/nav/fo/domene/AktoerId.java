package no.nav.fo.domene;

import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class AktoerId {
    String aktoerId;

    public AktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    @Override
    public String toString() {
        return aktoerId;
    }
}
