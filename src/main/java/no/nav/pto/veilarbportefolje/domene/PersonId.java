package no.nav.pto.veilarbportefolje.domene;

import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value(staticConstructor = "of")
@Getter(value = PRIVATE)
public class PersonId {
    public final String personId;

    @Override
    public String toString() {
        return personId;
    }

    public int toInteger() {
        return Integer.parseInt(personId);
    }
}
