package no.nav.fo.domene;

import lombok.Getter;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class PersonId {
    public final String personId;

    @Override
    public String toString() {
        return personId;
    }
}