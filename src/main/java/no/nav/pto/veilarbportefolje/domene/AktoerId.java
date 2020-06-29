package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class AktoerId {
    public final String aktoerId;

    @JsonCreator
    public static AktoerId of(String aktoerId) {
        return new AktoerId(aktoerId);
    }

    @Override
    public String toString() {
        return aktoerId;
    }
}
