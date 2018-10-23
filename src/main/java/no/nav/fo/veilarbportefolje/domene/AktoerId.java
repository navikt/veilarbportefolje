package no.nav.fo.veilarbportefolje.domene;

import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value(staticConstructor = "of")
@Getter(value = PRIVATE)
public class AktoerId {
    public final String aktoerId;

    @Override
    public String toString() {
        return aktoerId;
    }
}
