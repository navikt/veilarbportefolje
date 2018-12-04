package no.nav.fo.veilarbportefolje.domene;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value(staticConstructor = "of")
@EqualsAndHashCode
@Getter(value = PRIVATE)
public class PersonId {
    public final String personId;

    @Override
    public String toString() {
        return personId;
    }
}
