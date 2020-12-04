package no.nav.pto.veilarbportefolje.domene.value;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class Fnr extends ValueObject<String> {

    @JsonCreator
    public Fnr(String fnr) {
        super(fnr);
        if (!fnr.matches("\\d{11}")) {
            throw new IllegalArgumentException("Fødselsnummeret er ugyldig");
        }
    }

    public static Fnr of(String fnr) {
        return new Fnr(fnr);
    }
}
