package no.nav.pto.veilarbportefolje.arenapakafka.ytelser;

import no.nav.pto.veilarbportefolje.domene.ValueObject;

public final class PersonId extends ValueObject<String> {
    public PersonId(String value) {
        super(value);
    }

    public static PersonId of(String value) {
        return new PersonId(value);
    }

    public int toInteger() {
        return Integer.parseInt(value);
    }
}
